package com.group1.banking.repository;

import com.group1.banking.entity.Customer;
import com.group1.banking.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for CustomerRepository using @DataJpaTest.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("Jane Doe");
        customer.setAddress("456 Oak Ave");
        customer.setType(CustomerType.PERSON);
        customer.setDateOfBirth(LocalDate.of(1985, 6, 15));
        savedCustomer = customerRepository.save(customer);
    }

    @Test
    void save_shouldPersistCustomer_withGeneratedId() {
        assertThat(savedCustomer.getCustomerId()).isNotNull();
        assertThat(savedCustomer.getName()).isEqualTo("Jane Doe");
    }

    @Test
    void findByCustomerIdAndDeletedAtIsNull_shouldReturnCustomer_whenNotDeleted() {
        Optional<Customer> found = customerRepository.findByCustomerIdAndDeletedAtIsNull(savedCustomer.getCustomerId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void findByCustomerIdAndDeletedAtIsNull_shouldReturnEmpty_whenDeleted() {
        savedCustomer.setDeletedAt(Instant.now());
        customerRepository.save(savedCustomer);

        Optional<Customer> found = customerRepository.findByCustomerIdAndDeletedAtIsNull(savedCustomer.getCustomerId());
        assertThat(found).isEmpty();
    }

    @Test
    void existsByCustomerIdAndDeletedAtIsNull_shouldReturnTrue_whenExists() {
        boolean exists = customerRepository.existsByCustomerIdAndDeletedAtIsNull(savedCustomer.getCustomerId());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByCustomerIdAndDeletedAtIsNull_shouldReturnFalse_whenDeleted() {
        savedCustomer.setDeletedAt(Instant.now());
        customerRepository.save(savedCustomer);

        boolean exists = customerRepository.existsByCustomerIdAndDeletedAtIsNull(savedCustomer.getCustomerId());
        assertThat(exists).isFalse();
    }

    @Test
    void createdAt_shouldBeSetAutomatically() {
        assertThat(savedCustomer.getCreatedAt()).isNotNull();
    }

    @Test
    void updatedAt_shouldBeSetAutomatically() {
        assertThat(savedCustomer.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        Optional<Customer> found = customerRepository.findById(999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void delete_shouldRemoveCustomer() {
        customerRepository.delete(savedCustomer);
        Optional<Customer> found = customerRepository.findById(savedCustomer.getCustomerId());
        assertThat(found).isEmpty();
    }

    @Test
    void customer_shouldSupportCompanyType() {
        Customer company = new Customer();
        company.setName("ACME Corp");
        company.setAddress("789 Business Blvd");
        company.setType(CustomerType.COMPANY);
        company.setDateOfBirth(LocalDate.of(2000, 1, 1));
        Customer saved = customerRepository.save(company);

        Optional<Customer> found = customerRepository.findById(saved.getCustomerId());
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(CustomerType.COMPANY);
    }
}
