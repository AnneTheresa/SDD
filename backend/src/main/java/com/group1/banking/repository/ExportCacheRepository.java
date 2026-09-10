package com.group1.banking.repository;
import com.group1.banking.entity.ExportCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ExportCacheRepository extends JpaRepository<ExportCacheEntity, Long> {
    Optional<ExportCacheEntity> findByAccountIdAndParamHash(Long accountId, String paramHash);
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
