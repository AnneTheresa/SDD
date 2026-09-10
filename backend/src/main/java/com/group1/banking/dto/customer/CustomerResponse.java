package com.group1.banking.dto.customer;

import com.group1.banking.enums.CustomerType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class CustomerResponse {
    private Long customerId;
    private String name;
    private String address;
    private CustomerType type;
    private LocalDate dateOfBirth;
    private boolean kycVerified;
    private List<AccountResponse> accounts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Long getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public CustomerType getType() { return type; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean isKycVerified() { return kycVerified; }
    public List<AccountResponse> getAccounts() { return accounts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long customerId;
        private String name;
        private String address;
        private CustomerType type;
        private LocalDate dateOfBirth;
        private boolean kycVerified;
        private List<AccountResponse> accounts;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant deletedAt;

        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder type(CustomerType type) { this.type = type; return this; }
        public Builder dateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; return this; }
        public Builder kycVerified(boolean kycVerified) { this.kycVerified = kycVerified; return this; }
        public Builder accounts(List<AccountResponse> accounts) { this.accounts = accounts; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder deletedAt(Instant deletedAt) { this.deletedAt = deletedAt; return this; }

        public CustomerResponse build() {
            CustomerResponse r = new CustomerResponse();
            r.customerId = this.customerId;
            r.name = this.name;
            r.address = this.address;
            r.type = this.type;
            r.dateOfBirth = this.dateOfBirth;
            r.kycVerified = this.kycVerified;
            r.accounts = this.accounts;
            r.createdAt = this.createdAt;
            r.updatedAt = this.updatedAt;
            r.deletedAt = this.deletedAt;
            return r;
        }
    }
}