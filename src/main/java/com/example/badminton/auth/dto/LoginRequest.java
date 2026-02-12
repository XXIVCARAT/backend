package com.example.badminton.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank(message = "Password invalid") String password
) {}
