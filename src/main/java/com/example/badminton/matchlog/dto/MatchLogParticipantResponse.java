package com.example.badminton.matchlog.dto;

public record MatchLogParticipantResponse(
        Long userId,
        String username,
        String teamSide,
        String decision
) {}
