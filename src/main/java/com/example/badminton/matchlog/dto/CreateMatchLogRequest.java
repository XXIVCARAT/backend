package com.example.badminton.matchlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateMatchLogRequest(
        @NotBlank @Size(max = 120) String matchName,
        @NotBlank String matchFormat,
        @NotBlank String winnerSide,
        @Size(max = 120) String points,
        @NotEmpty List<Long> teamUserIds,
        @NotEmpty List<Long> opponentUserIds
) {}
