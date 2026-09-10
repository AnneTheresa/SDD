package com.group1.banking.dto.customer;

import com.group1.banking.enums.CustomerType;
import jakarta.validation.constraints.Size;
public class PatchCustomerRequest {

    @Size(min = 2, message = "name must be at least 2 characters")
    private String name;

    private String address;

    private CustomerType type;

    // Explicitly blocked by the service layer per spec.
    private String email;
    private String accountNumber;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public CustomerType getType() { return type; }
    public void setType(CustomerType type) { this.type = type; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
}
