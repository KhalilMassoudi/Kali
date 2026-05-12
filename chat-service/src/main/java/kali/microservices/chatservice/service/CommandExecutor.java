package kali.microservices.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dispatche l'action détectée par OpenAI vers le service approprié.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutor {

    private final InfrastructureClient infrastructureClient;
    private final SupportClient supportClient;

    /**
     * Exécute l'action et retourne un résultat brut (serialisé en JSON par le ChatService).
     */
    public Object execute(CommandIntent intent, Long userId) {
        String action = intent.getAction();
        Map<String, Object> params = intent.getParams();

        log.info("Exécution action='{}' pour userId={}", action, userId);

        try {
            return switch (action) {
                // ── VPS ──────────────────────────────────────────────────────
                case "create_vps"     -> infrastructureClient.createVps(params, userId);
                case "list_vps"       -> infrastructureClient.listVps(userId);
                case "delete_vps"     -> {
                    Long vpsId = toLong(params.get("vpsId"));
                    infrastructureClient.deleteVps(vpsId);
                    yield Map.of("message", "VPS supprimé avec succès");
                }

                // ── DOMAINES ─────────────────────────────────────────────────
                case "create_domain"  -> infrastructureClient.createDomain(params, userId);
                case "list_domains"   -> infrastructureClient.listDomains(userId);
                case "delete_domain"  -> {
                    Long domainId = toLong(params.get("domainId"));
                    infrastructureClient.deleteDomain(domainId);
                    yield Map.of("message", "Domaine supprimé avec succès");
                }

                // ── KUBERNETES ───────────────────────────────────────────────
                case "create_cluster" -> infrastructureClient.createCluster(params, userId);
                case "list_clusters"  -> infrastructureClient.listClusters(userId);

                // ── SUPPORT ──────────────────────────────────────────────────
                case "open_ticket"    -> supportClient.createTicket(params, userId);
                case "list_tickets"   -> supportClient.listTickets(userId);

                // ── AUTRES ───────────────────────────────────────────────────
                case "show_billing"   -> Map.of(
                        "message", "Accédez à la facturation via /api/billing/invoices/user/" + userId
                );
                default -> Map.of("message", "Action non reconnue: " + action);
            };
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de '{}': {}", action, e.getMessage());
            return Map.of("error", "Erreur lors de l'exécution: " + e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return 0L; }
    }
}
