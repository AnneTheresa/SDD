package com.group1.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group1.banking.entity.IdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
    void deleteByCreatedAtBefore(Instant cutoff);
    Optional<IdempotencyRecord> findByStorageKey(String storageKey);
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
