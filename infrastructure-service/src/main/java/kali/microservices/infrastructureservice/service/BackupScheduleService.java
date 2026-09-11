package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.BackupScheduleRequest;
import kali.microservices.infrastructureservice.entities.VpsBackupSchedule;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.ImageOption;
import kali.microservices.infrastructureservice.repository.VpsBackupScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupScheduleService {

    public static final String BACKUP_PREFIX = "backup-";

    private final VpsBackupScheduleRepository repository;
    private final CloudProviderFactory cloudProviderFactory;

    public VpsBackupSchedule get(Long vpsId) {
        return repository.findByVpsId(vpsId).orElse(null);
    }

    public VpsBackupSchedule upsert(Long vpsId, Long userId, BackupScheduleRequest request) {
        VpsBackupSchedule schedule = repository.findByVpsId(vpsId).orElseGet(VpsBackupSchedule::new);
        schedule.setVpsId(vpsId);
        schedule.setUserId(userId);
        schedule.setFrequency(request.getFrequency());
        schedule.setRetentionCount(request.getRetentionCount());
        schedule.setEnabled(request.getEnabled());
        if (schedule.getNextRunAt() == null) {
            schedule.setNextRunAt(nextRun(LocalDateTime.now(), request.getFrequency()));
        }
        return repository.save(schedule);
    }

    public void delete(Long vpsId) {
        repository.findByVpsId(vpsId).ifPresent(repository::delete);
    }

    public List<ImageOption> listBackups(Long vpsId) {
        return cloudProviderFactory.getProvider().listImagesByPrefix(BACKUP_PREFIX + vpsId + "-");
    }

    static LocalDateTime nextRun(LocalDateTime from, VpsBackupSchedule.Frequency frequency) {
        return frequency == VpsBackupSchedule.Frequency.WEEKLY ? from.plusWeeks(1) : from.plusDays(1);
    }
}
