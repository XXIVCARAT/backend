package com.example.badminton.stats;

import com.example.badminton.auth.AuthConstants;
import com.example.badminton.auth.JwtService;
import com.example.badminton.stats.dto.DashboardStatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class DashboardStatsController {
    private final DashboardStatsService dashboardStatsService;
    private final JwtService jwtService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService, JwtService jwtService) {
        this.dashboardStatsService = dashboardStatsService;
        this.jwtService = jwtService;
    }

    @GetMapping("/users/{userId}/stats")
    public DashboardStatsResponse getUserStats(
            @PathVariable Long userId,
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token
    ) {
        Long authenticatedUserId = jwtService.parseUserId(token == null ? "" : token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        if (!authenticatedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return dashboardStatsService.getDashboardStats(userId);
    }
}
