package com.example.badminton.matchlog.dto;

import java.time.Instant;
import java.util.List;

public record MatchHistoryItemResponse(
        Long id,
        String matchName,
        String matchFormat,
        String points,
        String winnerSide,
        String createdByUsername,
        Instant createdAt,
        boolean userWon,
        String userTeamSide,
        List<String> teamUsernames,
        List<String> opponentUsernames
) {}
