package kali.microservices.chatservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Agent conversationnel basé sur un modèle auto-hébergé via Ollama (function calling natif),
 * pour développement/tests locaux uniquement (pas de déploiement tant qu'aucun serveur GPU
 * n'est disponible). Même contrat que GeminiAgentService (mêmes outils, même garde de
 * confirmation) — seul le transport change : Ollama est sans état, on renvoie l'historique
 * complet à chaque appel plutôt qu'un identifiant d'interaction serveur.
 * API vérifiée en direct (curl) avant écriture : POST {url}/api/chat, voir
 * https://github.com/ollama/ollama/blob/main/docs/api.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaAgentService {

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    private final WebClient.Builder webClientBuilder;
    private final CommandExecutor commandExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_TOOL_ITERATIONS = 6;

    private static final Set<String> CONFIRMATION_REQUIRED = Set.of(
            "create_vps", "delete_vps", "stop_vps", "reboot_vps"
    );

    /** A short, unconditional "yes" to a proposal the model made in its previous turn. */
    private static final Pattern AFFIRMATIVE = Pattern.compile(
            "^\\s*(oui|yes|ok|okay|d'accord|vas-y|go|go ahead|confirme|confirm|confirmé|yep|yeah|sure|parfait)\\b.*",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_AFFIRMATIVE_WORDS = 5;

    /** The model claiming an action happened. Only allowed when a tool actually ran this turn. */
    private static final Pattern COMPLETION_CLAIM = Pattern.compile(
            "(j'ai (bien )?(créé|supprimé|lancé|déployé|ouvert|démarré|arrêté|stoppé|redémarré)"
                    + "|a été (créée?|supprimée?|déployée?|assignée?|démarrée?|arrêtée?|stoppée?|redémarrée?)"
                    + "|est (maintenant )?(en cours d'exécution|opérationnel)|est maintenant (démarrée?|arrêtée?|stoppée?|redémarrée?)"
                    + "|has been (created|deleted|deployed|assigned|started|stopped|rebooted|restarted)"
                    + "|i('ve| have) (gone ahead|created|deleted|deployed|started|stopped|rebooted|restarted)"
                    + "|is now (up|running|stopped))",
            Pattern.CASE_INSENSITIVE);

    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    /**
     * Last action the model proposed (called with confirmed=false), per user. When the user's
     * next message is a plain "yes", we execute it ourselves instead of trusting the model to
     * re-issue the call — local models regularly skip the call and just claim success.
     */
    private record PendingAction(String name, Map<String, Object> args, Instant at) {}
    private final Map<Long, PendingAction> pendingActions = new ConcurrentHashMap<>();

    private static final String SYSTEM_INSTRUCTION = """
            Tu es l'assistant IA de Safozi Cloud, une plateforme d'infrastructure cloud basée sur
            OpenStack. Tu peux : créer, lister, démarrer, arrêter, redémarrer et supprimer les VPS
            de l'utilisateur connecté, lister ses volumes de stockage (block storage), ouvrir et
            lister ses tickets de support, et afficher son solde de crédit et sa consommation récente.
            Tu ne gères PAS de noms de domaine ni de clusters Kubernetes (ces services n'existent
            pas sur la plateforme). Réponds en français ou en anglais selon la langue de l'utilisateur.

            RÈGLES ABSOLUES :
            - Pour proposer une création, une suppression, un arrêt ou un redémarrage de VPS, tu DOIS
              appeler l'outil correspondant avec confirmed=false. Ne propose jamais une telle action
              uniquement en texte. Le démarrage d'un VPS ne nécessite pas de confirmation.
            - Pour agir sur un VPS existant, utilise son identifiant numérique tel que renvoyé par
              list_vps. Si tu ne le connais pas avec certitude, appelle list_vps (la liste sera
              montrée à l'utilisateur) au lieu de deviner.
            - Ne mets confirmed=true QUE si l'utilisateur vient d'accepter explicitement (ex: "oui",
              "vas-y", "yes") l'action que tu as proposée dans TON message précédent.
            - Ne dis JAMAIS qu'une action a été effectuée si l'outil n'a pas été appelé et n'a pas
              renvoyé de succès. N'invente JAMAIS de nom, d'identifiant, d'adresse IP ou de statut :
              utilise uniquement les valeurs renvoyées par les outils.
            - Tu agis uniquement pour l'utilisateur connecté. Tu ne peux pas créer ni assigner de
              ressource à un autre client : dis-le honnêtement si on te le demande.
            - Ne parle pas de régions. Ne mentionne jamais le champ technique "confirmed".
            - Les actions de lecture (lister, afficher) n'ont pas besoin de confirmation.

            Sois concis et amical dans tes réponses.
            """;

    public record AgentResult(String text, String lastAction, Object lastActionData) {}

    private record ToolOutcome(Object data, boolean isError) {}

    public AgentResult chat(String userMessage, List<Map<String, String>> history, Long userId, String authHeader) {
        try {
            return runLoop(userMessage, history, userId, authHeader);
        } catch (Exception e) {
            log.error("Erreur agent Ollama: {}", e.getMessage(), e);
            return new AgentResult("Désolé, je rencontre des difficultés techniques. Réessayez dans un instant.", null, null);
        }
    }

    private AgentResult runLoop(String userMessage, List<Map<String, String>> history, Long userId, String authHeader) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", SYSTEM_INSTRUCTION));
        if (history != null) {
            for (Map<String, String> m : history) {
                String role = "assistant".equals(m.get("role")) ? "assistant" : "user";
                messages.add(msg(role, m.get("content")));
            }
        }
        messages.add(msg("user", userMessage));

        String lastAction = null;
        Object lastActionData = null;
        boolean nudged = false;

        PendingAction pending = pendingActions.remove(userId);
        if (pending != null && isPlainYes(userMessage) && Instant.now().isBefore(pending.at().plus(PENDING_TTL))) {
            Map<String, Object> args = new HashMap<>(pending.args());
            args.put("confirmed", true);
            ToolOutcome outcome = executeTool(pending.name(), args, userId, authHeader);
            lastAction = pending.name();
            lastActionData = outcome.data();
            messages.add(msg("system", "Le système vient d'exécuter l'action confirmée '" + pending.name()
                    + "'. Résultat réel (JSON) : " + toJson(outcome.data())
                    + (outcome.isError() ? " — c'est un ÉCHEC : explique l'erreur à l'utilisateur."
                    : " — informe l'utilisateur en te basant UNIQUEMENT sur ce résultat.")
                    + " N'appelle aucun outil pour cette même action."));
        }

        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("tools", TOOLS);
            requestBody.put("stream", false);

            JsonNode response = callChat(requestBody);
            JsonNode message = response.path("message");
            JsonNode toolCalls = message.path("tool_calls");
            String textOut = message.path("content").asText("");

            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                if (lastAction == null && COMPLETION_CLAIM.matcher(textOut).find()) {
                    if (!nudged) {
                        nudged = true;
                        log.warn("Agent claimed an action without calling any tool, retrying: {}", textOut);
                        messages.add(msg("system", "ATTENTION : tu n'as appelé aucun outil, donc RIEN n'a été "
                                + "exécuté. Appelle l'outil approprié (avec confirmed=false pour proposer), ou "
                                + "réponds honnêtement sans prétendre avoir agi."));
                        continue;
                    }
                    log.warn("Agent still claims an unexecuted action, replacing reply: {}", textOut);
                    return new AgentResult("Je n'ai effectué aucune action. Pouvez-vous reformuler votre demande "
                            + "(par exemple : « crée une VM avec 2 vCPU, 4 Go de RAM et 40 Go de disque ») ?",
                            null, null);
                }
                return new AgentResult(!textOut.isBlank() ? textOut : "D'accord.", lastAction, lastActionData);
            }

            // L'historique renvoyé au tour suivant doit contenir le tour assistant tel quel (avec ses tool_calls).
            Map<String, Object> assistantTurn = new LinkedHashMap<>();
            assistantTurn.put("role", "assistant");
            assistantTurn.put("content", textOut);
            assistantTurn.put("tool_calls", objectMapper.convertValue(toolCalls, new TypeReference<List<Map<String, Object>>>() {}));
            messages.add(assistantTurn);

            // Lectures répondues directement depuis les données (listes, solde) : laissé au
            // modèle, il supprimait des éléments et inventait des chiffres en test.
            boolean onlyDirect = true;
            List<String> directReplies = new ArrayList<>();
            String directAction = null;
            Object directData = null;
            for (JsonNode call : toolCalls) {
                JsonNode fn = call.path("function");
                String name = fn.path("name").asText();
                Map<String, Object> args = objectMapper.convertValue(fn.path("arguments"), new TypeReference<Map<String, Object>>() {});

                ToolOutcome outcome = executeTool(name, args, userId, authHeader);
                if (isPendingConfirmation(outcome)) {
                    pendingActions.put(userId, new PendingAction(name, args, Instant.now()));
                } else {
                    lastAction = name;
                    lastActionData = outcome.data();
                }

                String direct = outcome.isError() ? null : formatDirect(name, outcome.data());
                if (direct != null) {
                    directReplies.add(direct);
                    directAction = name;
                    directData = outcome.data();
                } else {
                    onlyDirect = false;
                }

                String resultJson = toJson(outcome.data());
                Map<String, Object> toolTurn = new LinkedHashMap<>();
                toolTurn.put("role", "tool");
                toolTurn.put("content", resultJson);
                toolTurn.put("tool_name", name);
                messages.add(toolTurn);
            }

            if (onlyDirect && !directReplies.isEmpty()) {
                return new AgentResult(String.join("\n\n", directReplies), directAction, directData);
            }
        }

        return new AgentResult("Désolé, cette demande est trop complexe pour l'instant. Pouvez-vous la reformuler en étapes plus simples ?", lastAction, lastActionData);
    }

    private ToolOutcome executeTool(String name, Map<String, Object> args, Long userId, String authHeader) {
        boolean needsConfirmation = CONFIRMATION_REQUIRED.contains(name);
        boolean confirmed = Boolean.TRUE.equals(args.get("confirmed"));

        if (needsConfirmation && !confirmed) {
            return new ToolOutcome(Map.of(
                    "status", "needs_confirmation",
                    "message", "Action non exécutée : décris précisément à l'utilisateur ce que tu proposes " +
                            "(avec les paramètres) et demande-lui de confirmer avant de rappeler cet outil avec confirmed=true."
            ), false);
        }

        Map<String, Object> params = new HashMap<>(args);
        params.remove("confirmed");

        CommandIntent intent = new CommandIntent();
        intent.setAction(name);
        intent.setParams(params);

        Object result = commandExecutor.execute(intent, userId, authHeader);
        boolean isError = result instanceof Map<?, ?> m && m.containsKey("error");
        return new ToolOutcome(result, isError);
    }

    /** Réponse formatée directement depuis les données pour les lectures, sinon null. */
    static String formatDirect(String toolName, Object data) {
        return switch (toolName) {
            case "list_vps" -> data instanceof List<?> l ? formatVpsList(l) : null;
            case "list_volumes" -> data instanceof List<?> l ? formatVolumeList(l) : null;
            case "show_billing" -> data instanceof Map<?, ?> m && !m.containsKey("error") ? formatBalance(m) : null;
            default -> null;
        };
    }

    static String formatVolumeList(List<?> volumes) {
        List<Map<?, ?>> active = volumes.stream()
                .filter(v -> v instanceof Map<?, ?> m && !"DELETED".equals(String.valueOf(m.get("status"))))
                .<Map<?, ?>>map(v -> (Map<?, ?>) v)
                .toList();
        if (active.isEmpty()) {
            return "Vous n'avez aucun volume de stockage pour le moment.";
        }
        StringBuilder sb = new StringBuilder("Vous avez " + active.size() + " volume" + (active.size() > 1 ? "s" : "") + " :\n");
        for (Map<?, ?> vol : active) {
            Object attached = vol.get("attachedVpsId");
            sb.append("\n• ").append(vol.get("name"))
                    .append(" (ID ").append(vol.get("id")).append(") — ").append(vol.get("sizeGb")).append(" Go")
                    .append(" — ").append(vol.get("status"))
                    .append(attached != null
                            ? " — attaché à la VM ID " + attached + (vol.get("device") != null ? " (" + vol.get("device") + ")" : "")
                            : " — non attaché");
        }
        return sb.toString();
    }

    private static final Map<String, String> RESOURCE_LABELS = Map.of(
            "VM_COMPUTE", "Calcul des VM",
            "VM_STORAGE", "Stockage des VM",
            "VOLUME", "Volumes",
            "FLOATING_IP", "IP flottantes"
    );

    static String formatBalance(Map<?, ?> summary) {
        Object currency = summary.get("currency") != null ? summary.get("currency") : "TND";
        String balance = money(summary.get("balance"));
        StringBuilder sb = new StringBuilder(balance != null
                ? "Votre solde actuel est de " + balance + " " + currency + "."
                : "Votre solde est indisponible pour le moment.");
        if (!summary.containsKey("usageTotal")) {
            sb.append("\n\nLe détail de votre consommation est indisponible pour le moment.");
            return sb.toString();
        }
        Object days = summary.get("usageWindowDays");
        String total = money(summary.get("usageTotal"));
        if (total == null || new BigDecimal(total).signum() == 0) {
            sb.append("\n\nAucune consommation facturée sur les ").append(days).append(" derniers jours.");
            return sb.toString();
        }
        sb.append("\n\nConsommation des ").append(days).append(" derniers jours : ")
                .append(total).append(" ").append(currency);
        if (summary.get("usageByResource") instanceof Map<?, ?> byResource && !byResource.isEmpty()) {
            sb.append(" :");
            byResource.forEach((k, v) -> sb.append("\n• ")
                    .append(RESOURCE_LABELS.getOrDefault(String.valueOf(k), String.valueOf(k)))
                    .append(" : ").append(money(v)).append(" ").append(currency));
        }
        return sb.toString();
    }

    private static String money(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String formatVpsList(List<?> vms) {
        List<Map<?, ?>> active = vms.stream()
                .filter(v -> v instanceof Map<?, ?> m && !"DELETED".equals(String.valueOf(m.get("status"))))
                .<Map<?, ?>>map(v -> (Map<?, ?>) v)
                .toList();
        if (active.isEmpty()) {
            return "Vous n'avez aucune VM pour le moment.";
        }
        StringBuilder sb = new StringBuilder("Vous avez " + active.size() + " VM" + (active.size() > 1 ? "s" : "") + " :\n");
        for (Map<?, ?> vm : active) {
            Object ram = vm.get("ram");
            String ramGb = ram instanceof Number n ? (n.intValue() / 1024) + " Go" : "?";
            Object ip = vm.get("floatingIp") != null ? vm.get("floatingIp") : vm.get("ipAddress");
            sb.append("\n• ").append(vm.get("name"))
                    .append(" (ID ").append(vm.get("id")).append(") — ").append(vm.get("status"))
                    .append(" — ").append(vm.get("cpu")).append(" vCPU, ").append(ramGb)
                    .append(", ").append(vm.get("storage")).append(" Go, ").append(vm.get("os"))
                    .append(ip != null ? ", IP " + ip : "");
        }
        return sb.toString();
    }

    private static boolean isPlainYes(String text) {
        return text != null && AFFIRMATIVE.matcher(text).matches()
                && text.trim().split("\\s+").length <= MAX_AFFIRMATIVE_WORDS;
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return String.valueOf(data);
        }
    }

    private boolean isPendingConfirmation(ToolOutcome outcome) {
        return outcome.data() instanceof Map<?, ?> m && "needs_confirmation".equals(m.get("status"));
    }

    private Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private JsonNode callChat(Map<String, Object> body) {
        WebClient client = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        String raw = client.post()
                .uri("/api/chat")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(b -> {
                            log.error("Ollama API error {}: {}", resp.statusCode(), b);
                            return new RuntimeException("Ollama error " + resp.statusCode() + ": " + b);
                        }))
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("Réponse Ollama illisible: " + raw, e);
        }
    }

    // ─────────────────────────────── Déclaration des outils (identique à GeminiAgentService) ───────────────────────────────

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static final Map<String, Object> CONFIRMED_PROP = Map.of(
            "type", "boolean",
            "description", "true UNIQUEMENT si l'utilisateur vient d'accepter explicitement cette action précise dans son dernier message ; sinon false."
    );

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", "function");
        t.put("function", function);
        return t;
    }

    private static final List<Map<String, Object>> TOOLS = List.of(
            tool("create_vps",
                    "Crée un nouveau serveur VPS pour l'utilisateur. Coûte des ressources réelles : nécessite confirmation explicite.",
                    Map.of(
                            "name", prop("string", "Nom du VPS (optionnel)"),
                            "os", prop("string", "Système d'exploitation, ex: ubuntu, debian, centos"),
                            "ram", prop("integer", "RAM en MB, ex: 2048 pour 2GB"),
                            "cpu", prop("integer", "Nombre de vCPU"),
                            "storage", prop("integer", "Stockage en GB"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("confirmed")),
            tool("list_vps", "Liste les VPS de l'utilisateur. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of()),
            tool("delete_vps",
                    "Supprime définitivement un VPS. Action irréversible : nécessite confirmation explicite.",
                    Map.of(
                            "vpsId", prop("integer", "Identifiant numérique du VPS à supprimer"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("vpsId", "confirmed")),
            tool("open_ticket",
                    "Ouvre un ticket de support. Action gratuite et réversible : aucune confirmation nécessaire.",
                    Map.of(
                            "title", prop("string", "Titre court du problème"),
                            "description", prop("string", "Description détaillée du problème"),
                            "priority", prop("string", "LOW, MEDIUM, HIGH ou URGENT"),
                            "category", prop("string", "Catégorie du problème")
                    ),
                    List.of("title")),
            tool("list_tickets", "Liste les tickets de support de l'utilisateur. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of()),
            tool("start_vps",
                    "Démarre un VPS arrêté. Aucune confirmation nécessaire.",
                    Map.of("vpsId", prop("integer", "Identifiant numérique du VPS à démarrer")),
                    List.of("vpsId")),
            tool("stop_vps",
                    "Arrête un VPS (le service hébergé devient indisponible) : nécessite confirmation explicite.",
                    Map.of(
                            "vpsId", prop("integer", "Identifiant numérique du VPS à arrêter"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("vpsId", "confirmed")),
            tool("reboot_vps",
                    "Redémarre un VPS (redémarrage forcé, interruption de service) : nécessite confirmation explicite.",
                    Map.of(
                            "vpsId", prop("integer", "Identifiant numérique du VPS à redémarrer"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("vpsId", "confirmed")),
            tool("list_volumes", "Liste les volumes de stockage (block storage) de l'utilisateur. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of()),
            tool("show_billing", "Affiche le solde de crédit de l'utilisateur et sa consommation récente. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of())
    );
}
