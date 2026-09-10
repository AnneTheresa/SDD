package com.group1.banking.repository;

import com.group1.banking.entity.Customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	
	Optional<Customer> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);
}
