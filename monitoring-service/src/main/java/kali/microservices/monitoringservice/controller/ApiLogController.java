package kali.microservices.monitoringservice.controller;

import jakarta.validation.Valid;
import kali.microservices.monitoringservice.dto.ApiLogIngestRequest;
import kali.microservices.monitoringservice.dto.PageResponse;
import kali.microservices.monitoringservice.entities.ApiLogEntry;
import kali.microservices.monitoringservice.security.AuthContext;
import kali.microservices.monitoringservice.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API access log — every request the Gateway proxies gets shipped here so admins can see
 * real API consumption (who called what, when, how it responded).
 */
@RestController
@RequestMapping("/api/monitoring/logs")
@RequiredArgsConstructor
public class ApiLogController {

    private final ApiLogService apiLogService;
    private final AuthContext authContext;

    /** Called by the Gateway only, right after it proxies a request. Not admin-gated — internal. */
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@Valid @RequestBody ApiLogIngestRequest request) {
        apiLogService.record(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<ApiLogEntry>> list(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "false") boolean errorsOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(PageResponse.from(apiLogService.search(service, errorsOnly, search, page, size)));
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> distinctServices(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(apiLogService.distinctServices());
    }
}
