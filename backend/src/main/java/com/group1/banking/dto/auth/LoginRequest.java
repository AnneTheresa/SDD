package com.group1.banking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "username is required")
    @Email(message = "username must be a valid email")
    private String username;

    @NotBlank(message = "password is required")
    private String password;
}
