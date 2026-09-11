package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.PageResponse;
import kali.microservices.infrastructureservice.entities.VpsActivityLog;
import kali.microservices.infrastructureservice.repository.VpsActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VpsActivityLogService {

    private final VpsActivityLogRepository repository;

    public void record(Long vpsId, Long userId, String action, String detail) {
        VpsActivityLog log = new VpsActivityLog();
        log.setVpsId(vpsId);
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        repository.save(log);
    }

    public PageResponse<VpsActivityLog> list(Long vpsId, int page, int size) {
        var result = repository.findByVpsIdOrderByCreatedAtDesc(vpsId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result);
    }
}
