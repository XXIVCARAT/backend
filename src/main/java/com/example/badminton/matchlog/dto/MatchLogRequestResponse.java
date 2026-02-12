package com.example.badminton.matchlog.dto;

import java.time.Instant;
import java.util.List;

public record MatchLogRequestResponse(
        Long id,
        String matchName,
        String matchFormat,
        String winnerSide,
        String points,
        String status,
        Long createdByUserId,
        String createdByUsername,
        Instant createdAt,
        boolean canRespond,
        List<MatchLogParticipantResponse> participants
) {}
