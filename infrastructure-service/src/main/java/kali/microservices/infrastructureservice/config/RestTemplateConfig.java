package kali.microservices.infrastructureservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Plain {@code new RestTemplate()} defaults to a request factory that streams POST
     * bodies without a Content-Length header (chunked transfer encoding). The Apache
     * front-end for this deployment's Keystone (and Cinder) rejects that outright with
     * "411 Length Required" — confirmed directly via a live call from TelemetryAuthService.
     * HttpComponentsClientHttpRequestFactory buffers the request entity and computes
     * Content-Length up front, which this backend requires.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
}
