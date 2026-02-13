package com.example.badminton.stats;

import com.example.badminton.auth.AuthConstants;
import com.example.badminton.auth.AuthSessionService;
import com.example.badminton.stats.dto.LeaderboardEntryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;
    private final AuthSessionService authSessionService;

    public LeaderboardController(LeaderboardService leaderboardService, AuthSessionService authSessionService) {
        this.leaderboardService = leaderboardService;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    public List<LeaderboardEntryResponse> leaderboard(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token
    ) {
        authSessionService.requireAuthenticatedUserId(token);
        return leaderboardService.getLeaderboard();
    }
}
