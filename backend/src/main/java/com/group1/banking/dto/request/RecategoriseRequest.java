package com.group1.banking.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for recategorising a transaction. (T095)
 * Validation against 8 allowed values is done in service layer for proper 422 response.
 */
public class RecategoriseRequest {

    @NotBlank(message = "category is required")
    private String category;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
