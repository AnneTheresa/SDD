package com.group1.banking.dto.customer;

import com.group1.banking.enums.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
public class CreateCustomerRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, message = "name must be at least 2 characters")
    private String name;

    @NotBlank(message = "address is required")
    private String address;

    @NotNull(message = "type is required")
    private CustomerType type;

    @NotNull(message = "dateOfBirth is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private java.time.LocalDate dateOfBirth;

    @JsonProperty
    private boolean kycVerified;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public CustomerType getType() { return type; }
    public void setType(CustomerType type) { this.type = type; }
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public boolean isKycVerified() { return kycVerified; }
    public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }
}
