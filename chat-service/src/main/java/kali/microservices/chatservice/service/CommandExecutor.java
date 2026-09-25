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
    private final BillingClient billingClient;

    /**
     * Exécute l'action et retourne un résultat brut (serialisé en JSON par le ChatService).
     * authHeader (le token du même appelant) est propagé vers infrastructure-service/
     * support-service, qui vérifient désormais l'identité indépendamment — sans lui, chaque
     * appel serait rejeté avec 401 depuis leur retrofit d'autorisation.
     */
    public Object execute(CommandIntent intent, Long userId, String authHeader) {
        String action = intent.getAction();
        Map<String, Object> params = intent.getParams();

        log.info("Exécution action='{}' pour userId={}", action, userId);

        try {
            return switch (action) {
                // ── VPS ──────────────────────────────────────────────────────
                case "create_vps"     -> infrastructureClient.createVps(params, userId, authHeader);
                case "list_vps"       -> infrastructureClient.listVps(userId, authHeader);
                case "delete_vps"     -> {
                    Long vpsId = toLong(params.get("vpsId"));
                    infrastructureClient.deleteVps(vpsId, authHeader);
                    yield Map.of("message", "VPS supprimé avec succès");
                }
                case "start_vps"      -> infrastructureClient.powerAction(toLong(params.get("vpsId")), "start", authHeader);
                case "stop_vps"       -> infrastructureClient.powerAction(toLong(params.get("vpsId")), "stop", authHeader);
                case "reboot_vps"     -> infrastructureClient.powerAction(toLong(params.get("vpsId")), "restart", authHeader);

                // ── STOCKAGE ─────────────────────────────────────────────────
                case "list_volumes"   -> infrastructureClient.listVolumes(userId, authHeader);

                // ── SUPPORT ──────────────────────────────────────────────────
                case "open_ticket"    -> supportClient.createTicket(params, authHeader);
                case "list_tickets"   -> supportClient.listTickets(userId, authHeader);

                // ── FACTURATION ──────────────────────────────────────────────
                case "show_billing"   -> billingClient.getBalanceSummary(userId, authHeader);
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
