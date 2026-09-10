package com.group1.banking.mapper;

import com.group1.banking.dto.customer.CustomerResponse;
import com.group1.banking.entity.Customer;
import com.group1.banking.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private CustomerMapper customerMapper;

    @BeforeEach
    void setUp() {
        customerMapper = new CustomerMapper();
    }

    @Test
    void toResponse_shouldMapCustomerId() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getCustomerId()).isEqualTo(10L);
    }

    @Test
    void toResponse_shouldMapName() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getName()).isEqualTo("Alice Smith");
    }

    @Test
    void toResponse_shouldMapAddress() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    void toResponse_shouldMapType() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getType()).isEqualTo(CustomerType.PERSON);
    }

    @Test
    void toResponse_shouldReturnEmptyAccountsList() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getAccounts()).isEmpty();
    }

    @Test
    void toResponse_shouldMapCreatedAt() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void toResponse_shouldMapUpdatedAt() {
        Customer customer = buildCustomer();
        CustomerResponse response = customerMapper.toResponse(customer);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    private Customer buildCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(10L);
        customer.setName("Alice Smith");
        customer.setAddress("123 Main St");
        customer.setType(CustomerType.PERSON);
        customer.setCreatedAt(Instant.now().minusSeconds(86400));
        customer.setUpdatedAt(Instant.now().minusSeconds(3600));
        return customer;
    }
}
