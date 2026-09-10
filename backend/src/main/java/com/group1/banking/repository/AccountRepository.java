package com.group1.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.AccountType;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountIdAndDeletedAtIsNull(Long accountId);

    List<Account> findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus(Long customerId, AccountStatus status);

    List<Account> findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatusNot(Long customerId, AccountStatus status);

    List<Account> findAllByDeletedAtIsNull();

    boolean existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus(Long customerId, AccountStatus status);

    Optional<Account> findByAccountId(Long accountNumber);

    boolean existsByCustomerCustomerIdAndAccountTypeAndDeletedAtIsNull(Long customerId, AccountType accountType);

}
