package kali.microservices.gatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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

    /** RestTemplate that never throws on 4xx/5xx — it forwards the error response as-is. */
    @Bean
    public RestTemplate proxyRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r) throws IOException {
                return false;
            }
        });
        return rt;
    }

    @Bean
    public RouterFunction<ServerResponse> routes(RestTemplate proxyRestTemplate) {
        return RouterFunctions.route()
                .route(RequestPredicates.path("/api/auth/**"),
                        req -> proxyTo(req, "http://localhost:8081", proxyRestTemplate))
                .route(RequestPredicates.path("/api/chat/**"),
                        req -> proxyTo(req, "http://localhost:8082", proxyRestTemplate))
                .route(RequestPredicates.path("/api/infrastructure/**"),
                        req -> proxyTo(req, "http://localhost:8083", proxyRestTemplate))
                .route(RequestPredicates.path("/api/billing/**"),
                        req -> proxyTo(req, "http://localhost:8084", proxyRestTemplate))
                .route(RequestPredicates.path("/api/support/**"),
                        req -> proxyTo(req, "http://localhost:8085", proxyRestTemplate))
                .route(RequestPredicates.path("/api/monitoring/**"),
                        req -> proxyTo(req, "http://localhost:8086", proxyRestTemplate))
                .build();
    }

    private static ServerResponse proxyTo(ServerRequest request, String baseUrl, RestTemplate rt)
            throws ServletException, IOException {

        // Build target URI (preserve path + query string)
        String query = request.uri().getRawQuery();
        URI target = URI.create(baseUrl + request.uri().getRawPath()
                + (query != null ? "?" + query : ""));

        // Read body (empty for GET/HEAD)
        byte[] body = null;
        try {
            body = request.body(byte[].class);
        } catch (Exception ignored) {}

        // Forward the request and capture the full response
        ResponseEntity<byte[]> response;
        try {
            response = rt.exchange(
                    new RequestEntity<>(body, request.headers().asHttpHeaders(), request.method(), target),
                    byte[].class);
        } catch (Exception e) {
            log.error("Proxy error forwarding to {}: {}", target, e.getMessage());
            return ServerResponse.status(502)
                    .body(("{\"error\":\"Gateway cannot reach upstream service\"}").getBytes());
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
