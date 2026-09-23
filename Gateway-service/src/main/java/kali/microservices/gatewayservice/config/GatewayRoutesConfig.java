package kali.microservices.gatewayservice.config;

import kali.microservices.gatewayservice.logging.ApiLogClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.*;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Slf4j
@Configuration
public class GatewayRoutesConfig {

    /**
     * RestTemplate that never throws on 4xx/5xx (forwards the error response as-is), backed by
     * JdkClientHttpRequestFactory rather than the default SimpleClientHttpRequestFactory —
     * the latter wraps HttpURLConnection, which cannot send PATCH requests at all (it throws
     * "Invalid HTTP method: PATCH" immediately, client-side, surfacing here as a bogus 502).
     */
    @Bean
    public RestTemplate proxyRestTemplate() {
        RestTemplate rt = new RestTemplate(new JdkClientHttpRequestFactory());
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r) throws IOException {
                return false;
            }
        });
        return rt;
    }

    @Value("${services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.chat.url:http://localhost:8082}")
    private String chatServiceUrl;

    @Value("${services.infrastructure.url:http://localhost:8083}")
    private String infrastructureServiceUrl;

    @Value("${services.billing.url:http://localhost:8084}")
    private String billingServiceUrl;

    @Value("${services.support.url:http://localhost:8085}")
    private String supportServiceUrl;

    @Value("${services.monitoring.url:http://localhost:8086}")
    private String monitoringServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> routes(RestTemplate proxyRestTemplate, ApiLogClient apiLogClient) {
        return RouterFunctions.route()
                .route(RequestPredicates.path("/api/auth/**"),
                        req -> proxyTo(req, authServiceUrl, "auth-service", proxyRestTemplate, apiLogClient))
                .route(RequestPredicates.path("/api/chat/**"),
                        req -> proxyTo(req, chatServiceUrl, "chat-service", proxyRestTemplate, apiLogClient))
                .route(RequestPredicates.path("/api/infrastructure/**"),
                        req -> proxyTo(req, infrastructureServiceUrl, "infrastructure-service", proxyRestTemplate, apiLogClient))
                .route(RequestPredicates.path("/api/billing/**"),
                        req -> proxyTo(req, billingServiceUrl, "billing-service", proxyRestTemplate, apiLogClient))
                .route(RequestPredicates.path("/api/support/**"),
                        req -> proxyTo(req, supportServiceUrl, "support-service", proxyRestTemplate, apiLogClient))
                .route(RequestPredicates.path("/api/monitoring/**"),
                        req -> proxyTo(req, monitoringServiceUrl, "monitoring-service", proxyRestTemplate, apiLogClient))
                .build();
    }

    private static ServerResponse proxyTo(ServerRequest request, String baseUrl, String serviceName,
                                           RestTemplate rt, ApiLogClient apiLogClient)
            throws ServletException, IOException {

        // Build target URI (preserve path + query string)
        String path = request.uri().getRawPath();
        String query = request.uri().getRawQuery();
        URI target = URI.create(baseUrl + path + (query != null ? "?" + query : ""));

        // Read body (empty for GET/HEAD)
        byte[] body = null;
        try {
            body = request.body(byte[].class);
        } catch (Exception ignored) {}

        // JwtAuthFilter only sets these as servlet request attributes (not real headers) once
        // the JWT is verified — re-emit them as real headers so downstream services can trust
        // the caller's identity/role without re-verifying the token themselves.
        HttpHeaders forwardHeaders = new HttpHeaders();
        forwardHeaders.addAll(request.headers().asHttpHeaders());
        Object userEmailAttr = request.attributes().get("X-User-Email");
        Object userRoleAttr = request.attributes().get("X-User-Role");
        String userEmail = userEmailAttr != null ? userEmailAttr.toString() : null;
        String userRole = userRoleAttr != null ? userRoleAttr.toString() : null;
        if (userEmail != null) forwardHeaders.set("X-User-Email", userEmail);
        if (userRole != null) forwardHeaders.set("X-User-Role", userRole);

        // Never log calls to the log-ingestion/read endpoints themselves — avoids recursive noise.
        boolean shouldLog = !path.startsWith("/api/monitoring/logs");
        long start = System.currentTimeMillis();

        // Forward the request and capture the full response
        ResponseEntity<byte[]> response;
        try {
            response = rt.exchange(
                    new RequestEntity<>(body, forwardHeaders, request.method(), target),
                    byte[].class);
        } catch (Exception e) {
            log.error("Proxy error forwarding to {}: {}", target, e.getMessage());
            if (shouldLog) {
                apiLogClient.log(request.method().name(), path, serviceName, userEmail, userRole,
                        502, System.currentTimeMillis() - start);
            }
            return ServerResponse.status(502)
                    .body(("{\"error\":\"Gateway cannot reach upstream service\"}").getBytes());
        }

        if (shouldLog) {
            apiLogClient.log(request.method().name(), path, serviceName, userEmail, userRole,
                    response.getStatusCode().value(), System.currentTimeMillis() - start);
        }

        // Build the response, stripping hop-by-hop headers
        return ServerResponse.status(response.getStatusCode().value())
                .headers(h -> response.getHeaders().forEach((name, values) -> {
                    if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                        h.put(name, values);
                    }
                }))
                .body(response.getBody() != null ? response.getBody() : new byte[0]);
    }

    /** Headers that must not be forwarded between proxies (RFC 7230). */
    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "transfer-encoding",
            "te", "trailers", "proxy-authorization", "proxy-authenticate", "upgrade"
    );
}
