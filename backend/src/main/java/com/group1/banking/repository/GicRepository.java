package com.group1.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group1.banking.entity.GicInvestment;
import com.group1.banking.entity.GicStatus;

public interface GicRepository extends JpaRepository<GicInvestment, String> {

    /** All non-deleted GICs for an account (active + redeemed but not soft-deleted). */
    List<GicInvestment> findAllByAccount_AccountIdAndDeletedAtIsNull(Long accountId);

    /** Check if any active (non-deleted) GIC exists — used when closing an RRSP account. */
    boolean existsByAccount_AccountIdAndDeletedAtIsNullAndStatus(Long accountId, GicStatus status);

    /** Lookup a specific active GIC by account + gicId for redeem. */
    Optional<GicInvestment> findByGicIdAndAccount_AccountIdAndDeletedAtIsNull(String gicId, Long accountId);
}
