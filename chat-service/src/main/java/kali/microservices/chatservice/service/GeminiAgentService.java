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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent conversationnel basé sur l'API "Interactions" de Gemini (function calling natif),
 * remplace l'ancienne approche OpenAiService (prompt "réponds en JSON" + parsing manuel).
 * Docs vérifiées en direct contre l'API réelle avant écriture (aucun schéma deviné) :
 * https://ai.google.dev/api/interactions-api
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAgentService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    private final WebClient.Builder webClientBuilder;
    private final CommandExecutor commandExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_TOOL_ITERATIONS = 6;

    /** Actions qui coûtent des ressources réelles ou sont irréversibles : confirmation obligatoire. */
    private static final Set<String> CONFIRMATION_REQUIRED = Set.of(
            "create_vps", "delete_vps", "create_domain", "delete_domain", "create_cluster"
    );

    private static final String SYSTEM_INSTRUCTION = """
            Tu es l'assistant IA de Kali Cloud, une plateforme d'infrastructure cloud (VPS, domaines,
            clusters Kubernetes, support). Utilise les outils fournis pour répondre aux demandes des
            utilisateurs, en français ou en anglais selon leur langue.

            RÈGLE DE CONFIRMATION : certains outils ont un champ "confirmed" (booléen).
            - Ne mets confirmed=true QUE si l'utilisateur vient d'exprimer un accord explicite
              (ex: "oui", "vas-y", "confirme", "yes", "go ahead") en réponse directe à une action
              que tu viens de proposer dans TON message précédent.
            - Sinon, mets confirmed=false. L'outil ne fera rien de réel dans ce cas : explique
              clairement à l'utilisateur ce que tu proposes de faire (avec les paramètres choisis)
              et demande-lui de confirmer. Ne dis JAMAIS qu'une action a été effectuée si elle ne
              l'a pas été.
            - Les actions de lecture (lister, afficher) n'ont pas besoin de confirmation.

            Sois concis et amical dans tes réponses.
            """;

    public record AgentResult(String text, String lastAction, Object lastActionData) {}

    private record ToolOutcome(Object data, boolean isError) {}

    public AgentResult chat(String userMessage, List<Map<String, String>> history, Long userId, String authHeader) {
        try {
            return runLoop(userMessage, history, userId, authHeader);
        } catch (Exception e) {
            log.error("Erreur agent Gemini: {}", e.getMessage(), e);
            return new AgentResult("Désolé, je rencontre des difficultés techniques. Réessayez dans un instant.", null, null);
        }
    }

    private AgentResult runLoop(String userMessage, List<Map<String, String>> history, Long userId, String authHeader) {
        List<Map<String, Object>> input = buildHistorySteps(history);
        input.add(step("user_input", userMessage));

        String interactionId = null;
        String lastAction = null;
        Object lastActionData = null;

        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            if (interactionId != null) {
                requestBody.put("previous_interaction_id", interactionId);
            } else {
                requestBody.put("system_instruction", SYSTEM_INSTRUCTION);
            }
            requestBody.put("input", input);
            requestBody.put("tools", TOOLS);
            requestBody.put("store", true);

            JsonNode response = callInteractions(requestBody);
            interactionId = response.path("id").asText();
            String status = response.path("status").asText();

            List<JsonNode> functionCalls = new ArrayList<>();
            String textOut = null;
            for (JsonNode s : response.path("steps")) {
                String type = s.path("type").asText();
                if ("function_call".equals(type)) {
                    functionCalls.add(s);
                } else if ("model_output".equals(type)) {
                    for (JsonNode c : s.path("content")) {
                        if ("text".equals(c.path("type").asText())) {
                            textOut = c.path("text").asText();
                        }
                    }
                }
            }

            if (!"requires_action".equals(status) || functionCalls.isEmpty()) {
                return new AgentResult(textOut != null && !textOut.isBlank() ? textOut : "D'accord.", lastAction, lastActionData);
            }

            List<Map<String, Object>> resultSteps = new ArrayList<>();
            for (JsonNode call : functionCalls) {
                String callId = call.path("id").asText();
                String name = call.path("name").asText();
                Map<String, Object> args = objectMapper.convertValue(call.path("arguments"), new TypeReference<Map<String, Object>>() {});

                ToolOutcome outcome = executeTool(name, args, userId, authHeader);
                if (!isPendingConfirmation(outcome)) {
                    lastAction = name;
                    lastActionData = outcome.data();
                }
                resultSteps.add(functionResultStep(callId, name, outcome.data(), outcome.isError()));
            }
            input = resultSteps;
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

    private boolean isPendingConfirmation(ToolOutcome outcome) {
        return outcome.data() instanceof Map<?, ?> m && "needs_confirmation".equals(m.get("status"));
    }

    // ─────────────────────────────── Requête HTTP ───────────────────────────────

    private JsonNode callInteractions(Map<String, Object> body) {
        WebClient client = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        String raw = client.post()
                .uri("/interactions")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(b -> {
                            log.error("Gemini Interactions API error {}: {}", resp.statusCode(), b);
                            return new RuntimeException("Gemini error " + resp.statusCode() + ": " + b);
                        }))
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("Réponse Gemini illisible: " + raw, e);
        }
    }

    // ─────────────────────────────── Construction des steps ───────────────────────────────

    private List<Map<String, Object>> buildHistorySteps(List<Map<String, String>> history) {
        List<Map<String, Object>> steps = new ArrayList<>();
        if (history != null) {
            for (Map<String, String> msg : history) {
                String type = "assistant".equals(msg.get("role")) ? "model_output" : "user_input";
                steps.add(step(type, msg.get("content")));
            }
        }
        return steps;
    }

    private Map<String, Object> step(String type, String text) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", type);
        s.put("content", List.of(Map.of("type", "text", "text", text)));
        return s;
    }

    private Map<String, Object> functionResultStep(String callId, String name, Object result, boolean isError) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "function_result");
        s.put("call_id", callId);
        s.put("name", name);
        try {
            s.put("result", objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            s.put("result", String.valueOf(result));
        }
        s.put("is_error", isError);
        return s;
    }

    // ─────────────────────────────── Déclaration des outils ───────────────────────────────

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

        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", "function");
        t.put("name", name);
        t.put("description", description);
        t.put("parameters", parameters);
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
                            "region", prop("string", "Région, ex: eu-west-1"),
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
            tool("create_domain",
                    "Enregistre un nouveau nom de domaine. Coûte des ressources réelles : nécessite confirmation explicite.",
                    Map.of(
                            "name", prop("string", "Nom de domaine, ex: monsite.tn"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("name", "confirmed")),
            tool("list_domains", "Liste les domaines de l'utilisateur. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of()),
            tool("delete_domain",
                    "Supprime définitivement un domaine. Action irréversible : nécessite confirmation explicite.",
                    Map.of(
                            "domainId", prop("integer", "Identifiant numérique du domaine à supprimer"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("domainId", "confirmed")),
            tool("create_cluster",
                    "Crée un cluster Kubernetes. Coûte des ressources réelles : nécessite confirmation explicite.",
                    Map.of(
                            "name", prop("string", "Nom du cluster (optionnel)"),
                            "nodeCount", prop("integer", "Nombre de nœuds"),
                            "kubernetesVersion", prop("string", "Version de Kubernetes, ex: 1.28"),
                            "region", prop("string", "Région"),
                            "confirmed", CONFIRMED_PROP
                    ),
                    List.of("confirmed")),
            tool("list_clusters", "Liste les clusters Kubernetes de l'utilisateur. Lecture seule, aucune confirmation nécessaire.",
                    Map.of(), List.of()),
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
            tool("show_billing", "Indique à l'utilisateur où consulter sa facturation.", Map.of(), List.of())
    );
}
