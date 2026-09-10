package com.group1.banking.repository;

import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.AccountType;
import com.group1.banking.entity.Customer;
import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionDirection;
import com.group1.banking.entity.TransactionStatus;
import com.group1.banking.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setType(CustomerType.PERSON);
        customer.setAddress("1 Test Street");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer = customerRepository.save(customer);

        account = new Account();
        account.setAccountId(5001L);
        account.setCustomer(customer);
        account.setAccountNumber("CA12345678901234567890");
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("1000.00"));
        account = accountRepository.save(account);
    }

    @Test
    void save_shouldPersistTransaction() {
        Transaction saved = transactionRepository.save(buildTransaction("tx-001"));
        assertThat(saved.getTransactionId()).isEqualTo("tx-001");
    }

    @Test
    void findById_shouldReturnTransaction_whenExists() {
        transactionRepository.save(buildTransaction("tx-002"));

        Optional<Transaction> found = transactionRepository.findById("tx-002");
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<Transaction> found = transactionRepository.findById("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldPersistDirection() {
        Transaction tx = buildTransaction("tx-003");
        tx.setDirection(TransactionDirection.CREDIT);
        transactionRepository.save(tx);

        Transaction found = transactionRepository.findById("tx-003").orElseThrow();
        assertThat(found.getDirection()).isEqualTo(TransactionDirection.CREDIT);
    }

    @Test
    void save_shouldPersistStatus() {
        Transaction tx = buildTransaction("tx-004");
        tx.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(tx);

        Transaction found = transactionRepository.findById("tx-004").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    private Transaction buildTransaction(String id) {
        Transaction tx = new Transaction();
        tx.setTransactionId(id);
        tx.setAccount(account);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setTimestamp(Instant.now());
        tx.setDescription("Test transaction");
        return tx;
    }
}
