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

    public Object createTicket(Map<String, Object> params, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", params.getOrDefault("title", "Nouveau ticket"));
        body.put("description", params.getOrDefault("description", params.getOrDefault("title", "")));
        body.put("priority", params.getOrDefault("priority", "MEDIUM"));
        body.put("category", params.getOrDefault("category", "OTHER"));

        log.info("Création ticket support: {}", body);
        ResponseEntity<Object> response = restTemplate.exchange(
                supportUrl + "/api/support/tickets",
                HttpMethod.POST,
                new HttpEntity<>(body, headers(authHeader)),
                Object.class
        );
        return response.getBody();
    }

    public List<?> listTickets(Long userId, String authHeader) {
        log.info("Liste tickets pour userId={}", userId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                supportUrl + "/api/support/tickets/user/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers(authHeader)),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    private HttpHeaders headers(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
