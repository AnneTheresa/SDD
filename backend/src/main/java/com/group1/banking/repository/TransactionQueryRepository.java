package com.group1.banking.repository;

import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionDirection;
import com.group1.banking.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

/**
 * Group 3-owned query repository for transactions. (T025)
 * Does not redefine the TransactionEntity — extends Group 2 repository
 * patterns.
 */
@Repository
public interface TransactionQueryRepository extends JpaRepository<Transaction, String> {

        List<Transaction> findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                        Long accountId, Instant start, Instant end);

        @Query("SELECT t FROM Transaction t WHERE t.account.accountId = :accountId " +
                        "AND t.direction IN :directions AND t.status = :status " +
                        "AND t.timestamp >= :start AND t.timestamp < :end " +
                        "ORDER BY t.timestamp ASC")
        List<Transaction> findEligibleForInsights(
                        @Param("accountId") Long accountId,
                        @Param("directions") List<TransactionDirection> directions,
                        @Param("status") TransactionStatus status,
                        @Param("start") Instant start,
                        @Param("end") Instant end);

}
