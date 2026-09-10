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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for TransactionQueryRepository using @DataJpaTest.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransactionQueryRepositoryTest {

    @Autowired
    private TransactionQueryRepository transactionQueryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Account account;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setAddress("123 Test St");
        customer.setType(CustomerType.PERSON);
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        Customer savedCustomer = customerRepository.save(customer);

        account = new Account();
        account.setAccountId(2001L);
        account.setCustomer(savedCustomer);
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAccountNumber("ACC-2001");
        account.setDailyTransferLimit(new BigDecimal("3000.00"));
        accountRepository.save(account);

        // Insert a CREDIT transaction
        Transaction tx1 = new Transaction();
        tx1.setTransactionId("tx-credit-001");
        tx1.setAccount(account);
        tx1.setAmount(new BigDecimal("200.00"));
        tx1.setDirection(TransactionDirection.CREDIT);
        tx1.setStatus(TransactionStatus.SUCCESS);
        tx1.setTimestamp(now.minusSeconds(3600));
        tx1.setDescription("Salary");
        transactionQueryRepository.save(tx1);

        // Insert a DEBIT transaction
        Transaction tx2 = new Transaction();
        tx2.setTransactionId("tx-debit-001");
        tx2.setAccount(account);
        tx2.setAmount(new BigDecimal("50.00"));
        tx2.setDirection(TransactionDirection.DEBIT);
        tx2.setStatus(TransactionStatus.SUCCESS);
        tx2.setTimestamp(now.minusSeconds(1800));
        tx2.setDescription("Grocery store");
        transactionQueryRepository.save(tx2);

        // Insert a FAILED transaction
        Transaction tx3 = new Transaction();
        tx3.setTransactionId("tx-failed-001");
        tx3.setAccount(account);
        tx3.setAmount(new BigDecimal("5000.00"));
        tx3.setDirection(TransactionDirection.DEBIT);
        tx3.setStatus(TransactionStatus.FAILED);
        tx3.setTimestamp(now.minusSeconds(900));
        tx3.setDescription("Large purchase");
        transactionQueryRepository.save(tx3);
    }

    @Test
    void findByAccount_AccountIdAndTimestampBetween_shouldReturnAllTransactions() {
        Instant start = now.minusSeconds(7200);
        Instant end = now;

        List<Transaction> txns = transactionQueryRepository
                .findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getAccountId(), start, end);

        assertThat(txns).hasSize(3);
    }

    @Test
    void findByAccount_AccountIdAndTimestampBetween_shouldRespectDateRange() {
        Instant start = now.minusSeconds(2000);
        Instant end = now;

        List<Transaction> txns = transactionQueryRepository
                .findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getAccountId(), start, end);

        // Only the debit and failed txns fall within this window
        assertThat(txns).hasSize(2);
    }

    @Test
    void findByAccount_AccountIdAndTimestampBetween_shouldReturnOrderedByTimestamp() {
        Instant start = now.minusSeconds(7200);
        Instant end = now;

        List<Transaction> txns = transactionQueryRepository
                .findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getAccountId(), start, end);

        // First tx should be earliest
        assertThat(txns.get(0).getTransactionId()).isEqualTo("tx-credit-001");
        assertThat(txns.get(2).getTransactionId()).isEqualTo("tx-failed-001");
    }

    @Test
    void findByAccount_AccountIdAndTimestampBetween_shouldReturnEmpty_whenNoMatch() {
        Instant start = now.minusSeconds(100000);
        Instant end = now.minusSeconds(50000);

        List<Transaction> txns = transactionQueryRepository
                .findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getAccountId(), start, end);

        assertThat(txns).isEmpty();
    }

    @Test
    void findEligibleForInsights_shouldReturnOnlyDebitSuccess() {
        Instant start = now.minusSeconds(7200);
        Instant end = now;

        List<Transaction> txns = transactionQueryRepository.findEligibleForInsights(
                account.getAccountId(),
                List.of(TransactionDirection.DEBIT, TransactionDirection.TRANSFER),
                TransactionStatus.SUCCESS,
                start, end);

        // Only the SUCCESS DEBIT — the FAILED and CREDIT are excluded
        assertThat(txns).hasSize(1);
        assertThat(txns.get(0).getTransactionId()).isEqualTo("tx-debit-001");
    }

    @Test
    void findEligibleForInsights_shouldReturnEmpty_whenNoEligible() {
        Instant start = now.minusSeconds(7200);
        Instant end = now;

        List<Transaction> txns = transactionQueryRepository.findEligibleForInsights(
                account.getAccountId(),
                List.of(TransactionDirection.TRANSFER), // no TRANSFER txns exist
                TransactionStatus.SUCCESS,
                start, end);

        assertThat(txns).isEmpty();
    }

    @Test
    void save_shouldPersistTransaction_withAllFields() {
        Transaction tx = new Transaction();
        tx.setTransactionId("tx-full-001");
        tx.setAccount(account);
        tx.setAmount(new BigDecimal("75.25"));
        tx.setDirection(TransactionDirection.CREDIT);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setTimestamp(now);
        tx.setDescription("Test deposit");
        tx.setCategory("Income");
        transactionQueryRepository.save(tx);

        assertThat(transactionQueryRepository.findById("tx-full-001")).isPresent();
    }
}
