package com.example.badminton.stats.dto;

public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        int rating,
        int wins,
        int losses,
        int winRate,
        String tier
) {}
