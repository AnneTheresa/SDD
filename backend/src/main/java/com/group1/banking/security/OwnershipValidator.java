package com.group1.banking.security;

import com.group1.banking.exception.OwnershipException;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.entity.Account;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates that a caller owns the requested account. (T011)
 * ADMIN role bypasses the check; CUSTOMER must own the account.
 */
@Component
public class OwnershipValidator {

    private final AccountRepository accountRepository;

    public OwnershipValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Asserts ownership of accountId by the caller.
     * @param accountId the account to check
     * @param caller    the authenticated user
     * @throws OwnershipException if CUSTOMER does not own the account
     */
    @Transactional(readOnly = true)
    public void assertOwnership(long accountId, UserPrincipal caller) {
        if (caller.isAdmin()) {
            return; // ADMIN bypasses check
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new OwnershipException(
                        "Caller does not own account: " + accountId));
        Long owningCustomerId = account.getCustomer().getCustomerId();
        if (!owningCustomerId.equals(caller.getCustomerId())) {
            throw new OwnershipException("Caller does not own account: " + accountId);
        }
     
    }
}
