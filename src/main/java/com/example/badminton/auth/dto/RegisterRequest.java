package com.example.badminton.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String username,
        @NotBlank(message = "Account does not exist Or Password invalid") @Size(min = 6, max = 72, message = "Account does not exist Or Password invalid") String password
) {}
