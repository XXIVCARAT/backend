package com.example.badminton.matchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record MatchLogDecisionRequest(@NotBlank String decision) {}
