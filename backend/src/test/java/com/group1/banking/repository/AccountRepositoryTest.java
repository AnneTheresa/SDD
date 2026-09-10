package com.group1.banking.repository;

import com.group1.banking.entity.*;
import com.group1.banking.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for AccountRepository using @DataJpaTest (in-memory H2).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer savedCustomer;
    private Account activeAccount;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setAddress("123 Main St");
        customer.setType(CustomerType.PERSON);
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedCustomer = customerRepository.save(customer);

        activeAccount = new Account();
        activeAccount.setAccountId(1001L);
        activeAccount.setCustomer(savedCustomer);
        activeAccount.setAccountType(AccountType.CHECKING);
        activeAccount.setStatus(AccountStatus.ACTIVE);
        activeAccount.setBalance(new BigDecimal("500.00"));
        activeAccount.setAccountNumber("ACC-1001");
        activeAccount.setDailyTransferLimit(new BigDecimal("3000.00"));
        accountRepository.save(activeAccount);
    }

    @Test
    void save_shouldPersistAccount() {
        Optional<Account> found = accountRepository.findById(1001L);
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void findByAccountIdAndDeletedAtIsNull_shouldReturnAccount_whenNotDeleted() {
        Optional<Account> found = accountRepository.findByAccountIdAndDeletedAtIsNull(1001L);
        assertThat(found).isPresent();
        assertThat(found.get().getAccountId()).isEqualTo(1001L);
    }

    @Test
    void findByAccountIdAndDeletedAtIsNull_shouldReturnEmpty_whenDeleted() {
        activeAccount.setDeletedAt(Instant.now());
        accountRepository.save(activeAccount);

        Optional<Account> found = accountRepository.findByAccountIdAndDeletedAtIsNull(1001L);
        assertThat(found).isEmpty();
    }

    @Test
    void findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus_shouldReturnActiveAccounts() {
        List<Account> accounts = accountRepository.findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus(
                savedCustomer.getCustomerId(), AccountStatus.ACTIVE);

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus_shouldExcludeClosedAccounts() {
        Account closed = new Account();
        closed.setAccountId(1002L);
        closed.setCustomer(savedCustomer);
        closed.setAccountType(AccountType.SAVINGS);
        closed.setStatus(AccountStatus.CLOSED);
        closed.setBalance(BigDecimal.ZERO);
        closed.setAccountNumber("ACC-1002");
        closed.setDailyTransferLimit(new BigDecimal("3000.00"));
        accountRepository.save(closed);

        List<Account> accounts = accountRepository.findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus(
                savedCustomer.getCustomerId(), AccountStatus.ACTIVE);

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountId()).isEqualTo(1001L);
    }

    @Test
    void existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus_shouldReturnTrue_whenExists() {
        boolean exists = accountRepository.existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus(
                savedCustomer.getCustomerId(), AccountStatus.ACTIVE);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus_shouldReturnFalse_whenNone() {
        boolean exists = accountRepository.existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus(
                savedCustomer.getCustomerId(), AccountStatus.CLOSED);
        assertThat(exists).isFalse();
    }

    @Test
    void findByAccountId_shouldReturnAccount() {
        Optional<Account> found = accountRepository.findByAccountId(1001L);
        assertThat(found).isPresent();
    }

    @Test
    void createdAt_shouldBeSetAutomatically() {
        Optional<Account> found = accountRepository.findById(1001L);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
