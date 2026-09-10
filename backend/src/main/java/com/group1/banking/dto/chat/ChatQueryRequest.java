package com.group1.banking.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatQueryRequest {

    @JsonProperty("message")
    @NotBlank(message = "message is required")
    @Size(max = 1000, message = "message must be 1000 characters or fewer")
    private String message;

    public ChatQueryRequest() {}

    public ChatQueryRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
