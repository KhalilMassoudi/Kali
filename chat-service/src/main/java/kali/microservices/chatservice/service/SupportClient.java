package kali.microservices.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportClient {

    @Value("${services.support.url}")
    private String supportUrl;

    private final RestTemplate restTemplate;

    public Object createTicket(Map<String, Object> params, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("title", params.getOrDefault("title", "Nouveau ticket"));
        body.put("description", params.getOrDefault("description", params.getOrDefault("title", "")));
        body.put("priority", params.getOrDefault("priority", "MEDIUM"));
        body.put("category", params.getOrDefault("category", "OTHER"));

        log.info("Création ticket support: {}", body);
        return restTemplate.postForObject(supportUrl + "/api/support/tickets", body, Object.class);
    }

    public List<?> listTickets(Long userId) {
        log.info("Liste tickets pour userId={}", userId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                supportUrl + "/api/support/tickets/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}
