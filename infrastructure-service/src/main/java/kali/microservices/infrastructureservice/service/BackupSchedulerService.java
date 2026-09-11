package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.entities.VpsBackupSchedule;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.ImageOption;
import kali.microservices.infrastructureservice.repository.VpsBackupScheduleRepository;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fires due backup schedules — snapshots the VM, then prunes images beyond the configured
 * retention count. Hourly cadence matches this app's lack of any dedicated job-queue infra;
 * DAILY/WEEKLY schedules just check "is nextRunAt due yet" each tick.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupSchedulerService {

    private final VpsBackupScheduleRepository scheduleRepository;
    private final VpsServerRepository vpsServerRepository;
    private final CloudProviderFactory cloudProviderFactory;
    private final VpsActivityLogService activityLogService;

    @Scheduled(fixedRate = 3_600_000)
    public void runDueBackups() {
        List<VpsBackupSchedule> due = scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(LocalDateTime.now());
        for (VpsBackupSchedule schedule : due) {
            try {
                runOne(schedule);
            } catch (Exception e) {
                log.error("Scheduled backup failed for VPS {}: {}", schedule.getVpsId(), e.getMessage());
            }
        }
    }

    private void runOne(VpsBackupSchedule schedule) {
        VpsServer vps = vpsServerRepository.findById(schedule.getVpsId()).orElse(null);
        if (vps == null || vps.getExternalId() == null) {
            log.warn("Skipping backup for VPS {}: no OpenStack server associated", schedule.getVpsId());
            return;
        }

        String name = BackupScheduleService.BACKUP_PREFIX + schedule.getVpsId() + "-" + System.currentTimeMillis();
        cloudProviderFactory.getProvider().snapshotVPS(vps.getExternalId(), name);
        activityLogService.record(schedule.getVpsId(), schedule.getUserId(), "SCHEDULED_BACKUP", name);
        log.info("Scheduled backup created for VPS {}: {}", schedule.getVpsId(), name);

        prune(schedule);

        schedule.setLastRunAt(LocalDateTime.now());
        schedule.setNextRunAt(BackupScheduleService.nextRun(LocalDateTime.now(), schedule.getFrequency()));
        scheduleRepository.save(schedule);
    }

    private void prune(VpsBackupSchedule schedule) {
        List<ImageOption> backups = cloudProviderFactory.getProvider()
                .listImagesByPrefix(BackupScheduleService.BACKUP_PREFIX + schedule.getVpsId() + "-");
        // listImagesByPrefix returns newest-first (name embeds an increasing timestamp).
        if (backups.size() <= schedule.getRetentionCount()) return;
        for (ImageOption toDelete : backups.subList(schedule.getRetentionCount(), backups.size())) {
            try {
                cloudProviderFactory.getProvider().deleteImage(toDelete.id());
            } catch (Exception e) {
                log.error("Failed to prune backup image {}: {}", toDelete.id(), e.getMessage());
            }
        }
    }
}
