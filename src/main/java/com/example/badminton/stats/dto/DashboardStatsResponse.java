package com.example.badminton.stats.dto;

public record DashboardStatsResponse(
        Long userId,
        int rank,
        int rating,
        int matchesPlayed,
        int matchesWon,
        int matchesLost,
        int winRate,
        String tier
) {}
