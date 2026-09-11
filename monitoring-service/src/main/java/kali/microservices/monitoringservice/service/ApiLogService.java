package kali.microservices.monitoringservice.service;

import kali.microservices.monitoringservice.dto.ApiLogIngestRequest;
import kali.microservices.monitoringservice.entities.ApiLogEntry;
import kali.microservices.monitoringservice.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogService {

    private static final long RETENTION_HOURS = 24;

    private final ApiLogRepository apiLogRepository;

    public void record(ApiLogIngestRequest request) {
        ApiLogEntry entry = new ApiLogEntry();
        entry.setMethod(request.getMethod());
        entry.setPath(request.getPath());
        entry.setTargetService(request.getTargetService());
        entry.setUserEmail(request.getUserEmail());
        entry.setUserRole(request.getUserRole());
        entry.setStatusCode(request.getStatusCode());
        entry.setDurationMs(request.getDurationMs());
        apiLogRepository.save(entry);
    }

    public Page<ApiLogEntry> search(String service, boolean errorsOnly, String search, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        return apiLogRepository.search(
                blankToNull(service), errorsOnly, blankToNull(search), PageRequest.of(Math.max(page, 0), safeSize));
    }

    public List<String> distinctServices() {
        return apiLogRepository.findDistinctTargetServices();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Keeps the log table bounded — nothing older than 24h is kept. Runs hourly. */
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void purgeOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RETENTION_HOURS);
        int deleted = apiLogRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} API log entries older than {}h", deleted, RETENTION_HOURS);
        }
    }
}
