package com.group1.banking.service;

import java.util.List;

import com.group1.banking.dto.customer.CreateCustomerRequest;
import com.group1.banking.dto.customer.CustomerResponse;
import com.group1.banking.dto.customer.PatchCustomerRequest;

public interface CustomerService {
    CustomerResponse createCustomer(CreateCustomerRequest request);
    CustomerResponse updateCustomer(Long customerId, PatchCustomerRequest request);
    CustomerResponse getCustomer(Long customerId);
    List<CustomerResponse> getAllCustomers();
    void deleteCustomer(Long customerId);
}
