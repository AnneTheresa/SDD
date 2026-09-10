package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.customer.CreateCustomerRequest;
import com.group1.banking.dto.customer.CustomerResponse;
import com.group1.banking.dto.customer.PatchCustomerRequest;
import com.group1.banking.enums.CustomerType;
import com.group1.banking.exception.ResourceNotFoundException;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for CustomerController using @WebMvcTest.
 */
@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private CustomerResponse sampleResponse() {
        return CustomerResponse.builder()
                .customerId(42L)
                .name("Jane Doe")
                .address("123 Main St")
                .type(CustomerType.PERSON)
                .accounts(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ===== CREATE CUSTOMER =====

    @Test
    void createCustomer_shouldReturn201_whenValidRequest() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Jane Doe");
        request.setAddress("123 Main St");
        request.setType(CustomerType.PERSON);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.accounts").isArray());
    }

    @Test
    void createCustomer_shouldReturn400_whenNameMissing() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setAddress("123 Main St");
        request.setType(CustomerType.PERSON);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createCustomer_shouldReturn400_whenTypeMissing() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Jane Doe");
        request.setAddress("123 Main St");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createCustomer_shouldReturn400_whenNameTooShort() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("J"); // less than 2 chars
        request.setAddress("123 Main St");
        request.setType(CustomerType.PERSON);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    // ===== GET CUSTOMER =====

    @Test
    void getCustomer_shouldReturn200_whenFound() throws Exception {
        when(customerService.getCustomer(42L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/customers/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void getCustomer_shouldReturn404_whenNotFound() throws Exception {
        when(customerService.getCustomer(999L))
                .thenThrow(new ResourceNotFoundException("Customer not found", "CUSTOMER_NOT_FOUND", null));

        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound());
    }

    // ===== UPDATE CUSTOMER =====

    @Test
    void updateCustomer_shouldReturn200_whenValidPatch() throws Exception {
        PatchCustomerRequest request = new PatchCustomerRequest();
        request.setName("Jane Smith");

        CustomerResponse updated = CustomerResponse.builder()
                .customerId(42L)
                .name("Jane Smith")
                .address("123 Main St")
                .type(CustomerType.PERSON)
                .accounts(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(customerService.updateCustomer(eq(42L), any(PatchCustomerRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/customers/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Smith"));
    }

    // ===== GET ALL CUSTOMERS =====

    @Test
    void getAllCustomers_shouldReturn200_withList() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(42));
    }

    // ===== DELETE CUSTOMER =====

    @Test
    void deleteCustomer_shouldReturn200_withMessage() throws Exception {
        doNothing().when(customerService).deleteCustomer(42L);

        mockMvc.perform(delete("/api/customers/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));
    }
}
