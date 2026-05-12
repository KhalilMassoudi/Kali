package kali.microservices.chatservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.max-tokens}")
    private Integer maxTokens;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        Tu es un assistant expert pour gérer des services cloud sur la plateforme Kali Cloud.
        L'utilisateur peut demander en langage naturel (français ou anglais):
        - Créer/lister/supprimer des VPS
        - Gérer des domaines
        - Créer/lister des clusters Kubernetes
        - Ouvrir des tickets support
        - Voir sa facturation

        Réponds UNIQUEMENT en JSON valide avec ce format:
        {
            "action": "create_vps|list_vps|delete_vps|create_domain|list_domains|delete_domain|create_cluster|list_clusters|open_ticket|list_tickets|show_billing|clarify|none",
            "params": {
                "name": "mon-vps",
                "os": "ubuntu",
                "ram": 2048,
                "cpu": 2,
                "storage": 50,
                "region": "eu-west-1"
            },
            "response_message": "Message convivial pour l'utilisateur"
        }

        Exemples de mappings:
        - "Crée un VPS Ubuntu 4GB" → action: create_vps, params: {os: "ubuntu", ram: 4096}
        - "Liste mes serveurs" → action: list_vps
        - "Achète le domaine monsite.tn" → action: create_domain, params: {name: "monsite.tn"}
        - "Crée un cluster K8s 3 nœuds" → action: create_cluster, params: {nodeCount: 3}
        - "Ouvre un ticket, mon VPS est down" → action: open_ticket, params: {title: "VPS down", description: "..."}
        - "Bonjour" → action: none, response_message: "Bonjour! Comment puis-je vous aider?"
        - Si pas clair → action: clarify

        NE retourne QUE le JSON, rien d'autre.
        """;

    /**
     * Calls Gemini and parses the response into a CommandIntent.
     * Endpoint: POST {apiUrl}/{model}:generateContent
     * Auth: x-goog-api-key header
     */
    public CommandIntent parseCommand(String userMessage, List<Map<String, String>> history) {
        try {
            Map<String, Object> requestBody = buildGeminiRequest(userMessage, history);

            WebClient client = webClientBuilder
                    .baseUrl(apiUrl)
                    .defaultHeader("x-goog-api-key", apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String rawResponse = client.post()
                    .uri("/{model}:generateContent", model)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class).map(body -> {
                                log.error("Gemini API error {}: {}", response.statusCode(), body);
                                return new RuntimeException("Gemini error " + response.statusCode() + ": " + body);
                            }))
                    .bodyToMono(String.class)
                    .block();

            // Gemini response: candidates[0].content.parts[0].text
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("candidates")
                    .get(0).path("content").path("parts").get(0).path("text").asText();

            return parseIntent(content);

        } catch (Exception e) {
            log.error("Erreur Gemini: {}", e.getMessage());
            return fallbackIntent("Désolé, je rencontre des difficultés techniques. Réessayez dans un instant.");
        }
    }

    /**
     * Builds the Gemini request body.
     * - system_instruction: the system prompt
     * - contents: conversation history + current message
     * - generationConfig: force JSON output via responseMimeType
     */
    private Map<String, Object> buildGeminiRequest(String userMessage, List<Map<String, String>> history) {
        // System instruction (Gemini-specific, separate from contents)
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
        );

        // Conversation history — Gemini uses "model" for the assistant role
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            for (Map<String, String> msg : history) {
                String geminiRole = "assistant".equals(msg.get("role")) ? "model" : "user";
                contents.add(Map.of(
                        "role", geminiRole,
                        "parts", List.of(Map.of("text", msg.get("content")))
                ));
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        // Force JSON output
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "maxOutputTokens", maxTokens
        );

        return Map.of(
                "system_instruction", systemInstruction,
                "contents", contents,
                "generationConfig", generationConfig
        );
    }

    private CommandIntent parseIntent(String jsonContent) {
        try {
            JsonNode node = objectMapper.readTree(jsonContent);
            CommandIntent intent = new CommandIntent();
            intent.setAction(node.path("action").asText("none"));
            intent.setResponseMessage(node.path("response_message").asText(""));

            Map<String, Object> params = new HashMap<>();
            JsonNode paramsNode = node.path("params");
            if (paramsNode.isObject()) {
                paramsNode.fields().forEachRemaining(entry -> {
                    JsonNode val = entry.getValue();
                    if (val.isInt())          params.put(entry.getKey(), val.asInt());
                    else if (val.isLong())    params.put(entry.getKey(), val.asLong());
                    else if (val.isDouble())  params.put(entry.getKey(), val.asDouble());
                    else if (val.isBoolean()) params.put(entry.getKey(), val.asBoolean());
                    else                      params.put(entry.getKey(), val.asText());
                });
            }
            intent.setParams(params);
            return intent;

        } catch (Exception e) {
            log.warn("Impossible de parser la réponse Gemini: {}", jsonContent);
            return fallbackIntent("Je n'ai pas bien compris. Pouvez-vous reformuler?");
        }
    }

    private CommandIntent fallbackIntent(String message) {
        CommandIntent intent = new CommandIntent();
        intent.setAction("none");
        intent.setResponseMessage(message);
        intent.setParams(new HashMap<>());
        return intent;
    }
}
