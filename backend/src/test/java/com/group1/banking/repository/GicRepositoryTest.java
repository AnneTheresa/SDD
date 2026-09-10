package com.group1.banking.repository;

import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.AccountType;
import com.group1.banking.entity.Customer;
import com.group1.banking.entity.GicInvestment;
import com.group1.banking.entity.GicStatus;
import com.group1.banking.entity.GicTerm;
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

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GicRepositoryTest {

    @Autowired
    private GicRepository gicRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Account rrspAccount;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("Test User");
        customer.setAddress("123 Test St");
        customer.setType(CustomerType.PERSON);
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        Customer savedCustomer = customerRepository.save(customer);

        rrspAccount = new Account();
        rrspAccount.setAccountId(9001L);
        rrspAccount.setCustomer(savedCustomer);
        rrspAccount.setAccountType(AccountType.RRSP);
        rrspAccount.setStatus(AccountStatus.ACTIVE);
        rrspAccount.setBalance(new BigDecimal("10000.00"));
        rrspAccount.setAccountNumber("RRSP-9001");
        rrspAccount.setDailyTransferLimit(new BigDecimal("5000.00"));
        accountRepository.save(rrspAccount);
    }

    private GicInvestment buildGic(String gicId, GicStatus status, Instant deletedAt) {
        GicInvestment gic = new GicInvestment();
        gic.setGicId(gicId);
        gic.setAccount(rrspAccount);
        gic.setPrincipalAmount(new BigDecimal("1000.00"));
        gic.setInterestRate(GicTerm.ONE_YEAR.getAnnualRate());
        gic.setTerm(GicTerm.ONE_YEAR);
        gic.setStartDate(LocalDate.now());
        gic.setMaturityDate(LocalDate.now().plusYears(1));
        gic.setMaturityAmount(new BigDecimal("1050.00"));
        gic.setStatus(status);
        gic.setDeletedAt(deletedAt);
        return gic;
    }

    // ===== findAllByAccount_AccountIdAndDeletedAtIsNull =====

    @Test
    void findAll_shouldReturnActiveGics_whenNotDeleted() {
        gicRepository.save(buildGic("GIC-A001", GicStatus.ACTIVE, null));

        List<GicInvestment> result = gicRepository.findAllByAccount_AccountIdAndDeletedAtIsNull(9001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGicId()).isEqualTo("GIC-A001");
    }

    @Test
    void findAll_shouldExcludeSoftDeletedGics() {
        gicRepository.save(buildGic("GIC-D001", GicStatus.REDEEMED, Instant.now()));

        List<GicInvestment> result = gicRepository.findAllByAccount_AccountIdAndDeletedAtIsNull(9001L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMultipleGics_whenSeveralActive() {
        gicRepository.save(buildGic("GIC-A001", GicStatus.ACTIVE, null));
        gicRepository.save(buildGic("GIC-A002", GicStatus.ACTIVE, null));

        List<GicInvestment> result = gicRepository.findAllByAccount_AccountIdAndDeletedAtIsNull(9001L);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoGicsForAccount() {
        List<GicInvestment> result = gicRepository.findAllByAccount_AccountIdAndDeletedAtIsNull(9001L);

        assertThat(result).isEmpty();
    }

    // ===== existsByAccount_AccountIdAndDeletedAtIsNullAndStatus =====

    @Test
    void existsByStatus_shouldReturnTrue_whenActiveGicExists() {
        gicRepository.save(buildGic("GIC-A001", GicStatus.ACTIVE, null));

        boolean exists = gicRepository.existsByAccount_AccountIdAndDeletedAtIsNullAndStatus(9001L, GicStatus.ACTIVE);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByStatus_shouldReturnFalse_whenNoActiveGic() {
        boolean exists = gicRepository.existsByAccount_AccountIdAndDeletedAtIsNullAndStatus(9001L, GicStatus.ACTIVE);

        assertThat(exists).isFalse();
    }

    @Test
    void existsByStatus_shouldReturnFalse_whenActiveGicIsSoftDeleted() {
        gicRepository.save(buildGic("GIC-D001", GicStatus.ACTIVE, Instant.now()));

        boolean exists = gicRepository.existsByAccount_AccountIdAndDeletedAtIsNullAndStatus(9001L, GicStatus.ACTIVE);

        assertThat(exists).isFalse();
    }

    // ===== findByGicIdAndAccount_AccountIdAndDeletedAtIsNull =====

    @Test
    void findByGicId_shouldReturnGic_whenExists() {
        gicRepository.save(buildGic("GIC-A001", GicStatus.ACTIVE, null));

        Optional<GicInvestment> result = gicRepository.findByGicIdAndAccount_AccountIdAndDeletedAtIsNull("GIC-A001", 9001L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(GicStatus.ACTIVE);
    }

    @Test
    void findByGicId_shouldReturnEmpty_whenGicSoftDeleted() {
        gicRepository.save(buildGic("GIC-D001", GicStatus.REDEEMED, Instant.now()));

        Optional<GicInvestment> result = gicRepository.findByGicIdAndAccount_AccountIdAndDeletedAtIsNull("GIC-D001", 9001L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByGicId_shouldReturnEmpty_whenGicIdNotFound() {
        Optional<GicInvestment> result = gicRepository.findByGicIdAndAccount_AccountIdAndDeletedAtIsNull("NONEXISTENT", 9001L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByGicId_shouldReturnEmpty_whenAccountIdMismatch() {
        gicRepository.save(buildGic("GIC-A001", GicStatus.ACTIVE, null));

        Optional<GicInvestment> result = gicRepository.findByGicIdAndAccount_AccountIdAndDeletedAtIsNull("GIC-A001", 9999L);

        assertThat(result).isEmpty();
    }
}
