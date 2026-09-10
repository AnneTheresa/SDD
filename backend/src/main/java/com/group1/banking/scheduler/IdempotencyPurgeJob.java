package com.group1.banking.scheduler;

import com.group1.banking.repository.ExportCacheRepository;
import com.group1.banking.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled job to purge stale idempotency records and export cache entries. (T113)
 */
@Component
public class IdempotencyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurgeJob.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ExportCacheRepository exportCacheRepository;

    @Value("${banking.idempotency.purge-after-hours:72}")
    private int purgeAfterHours;

    public IdempotencyPurgeJob(IdempotencyRecordRepository idempotencyRecordRepository,
                                ExportCacheRepository exportCacheRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.exportCacheRepository = exportCacheRepository;
    }

    /**
     * Runs daily at 03:00 UTC.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void purge() {
        Instant cutoff = Instant.now().minus(purgeAfterHours, ChronoUnit.HOURS);
        idempotencyRecordRepository.deleteByCreatedAtBefore(cutoff);
        exportCacheRepository.deleteByCreatedAtBefore(LocalDateTime.from(cutoff));
        log.info("Idempotency purge completed: purged records older than {}h", purgeAfterHours);
    }
}
