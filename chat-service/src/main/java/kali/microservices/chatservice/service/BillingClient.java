package kali.microservices.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingClient {

    static final int USAGE_WINDOW_DAYS = 30;

    @Value("${services.billing.url}")
    private String billingUrl;

    private final RestTemplate restTemplate;

    /**
     * Solde du portefeuille + résumé de la consommation des 30 derniers jours, calculé à partir
     * des transactions réelles (USAGE_DEDUCTION) — jamais laissé au modèle.
     */
    public Map<String, Object> getBalanceSummary(Long userId, String authHeader) {
        log.info("Solde portefeuille pour userId={}", userId);
        ResponseEntity<Map<String, Object>> wallet = restTemplate.exchange(
                billingUrl + "/api/billing/wallet/user/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers(authHeader)),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = wallet.getBody() != null ? wallet.getBody() : Map.of();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("balance", body.get("balance"));
        summary.put("currency", body.get("currency") != null ? body.get("currency") : "TND");

        // Transactions isolées : un échec ici ne doit pas masquer le solde.
        try {
            ResponseEntity<List<Map<String, Object>>> txs = restTemplate.exchange(
                    billingUrl + "/api/billing/wallet/user/" + userId + "/transactions",
                    HttpMethod.GET,
                    new HttpEntity<>(headers(authHeader)),
                    new ParameterizedTypeReference<>() {}
            );
            summary.putAll(summarizeUsage(txs.getBody(), LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Transactions indisponibles pour userId={}: {}", userId, e.getMessage());
        }
        return summary;
    }

    /** Total et répartition par type de ressource des USAGE_DEDUCTION sur la fenêtre, en positif. */
    static Map<String, Object> summarizeUsage(List<Map<String, Object>> transactions, LocalDateTime now) {
        LocalDateTime since = now.minusDays(USAGE_WINDOW_DAYS);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byResource = new LinkedHashMap<>();
        if (transactions != null) {
            for (Map<String, Object> tx : transactions) {
                if (!"USAGE_DEDUCTION".equals(String.valueOf(tx.get("type")))) continue;
                LocalDateTime at = parseDate(tx.get("createdAt"));
                if (at == null || at.isBefore(since)) continue;
                BigDecimal amount = toDecimal(tx.get("amount"));
                if (amount == null) continue;
                amount = amount.abs();
                total = total.add(amount);
                String resource = tx.get("resourceType") != null ? String.valueOf(tx.get("resourceType")) : "OTHER";
                byResource.merge(resource, amount, BigDecimal::add);
            }
        }
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("usageWindowDays", USAGE_WINDOW_DAYS);
        usage.put("usageTotal", total.setScale(2, RoundingMode.HALF_UP));
        Map<String, Object> rounded = new LinkedHashMap<>();
        byResource.forEach((k, v) -> rounded.put(k, v.setScale(2, RoundingMode.HALF_UP)));
        usage.put("usageByResource", rounded);
        return usage;
    }

    /** LocalDateTime sérialisé par Jackson : chaîne ISO ou tableau [a, m, j, h, min, s, nanos]. */
    static LocalDateTime parseDate(Object value) {
        try {
            if (value instanceof String s) {
                return LocalDateTime.parse(s.length() > 26 ? s.substring(0, 26) : s);
            }
            if (value instanceof List<?> l && l.size() >= 3) {
                int[] p = new int[7];
                for (int i = 0; i < Math.min(l.size(), 7); i++) p[i] = ((Number) l.get(i)).intValue();
                return LocalDateTime.of(p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
            }
        } catch (Exception ignored) {}
        return null;
    }

    static BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private HttpHeaders headers(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
