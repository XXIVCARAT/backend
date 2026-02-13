package com.example.badminton.matchlog;

import com.example.badminton.auth.AuthConstants;
import com.example.badminton.auth.AuthSessionService;
import com.example.badminton.matchlog.dto.CreateMatchLogRequest;
import com.example.badminton.matchlog.dto.MatchLogDecisionRequest;
import com.example.badminton.matchlog.dto.MatchHistoryItemResponse;
import com.example.badminton.matchlog.dto.MatchLogRequestResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match-log/requests")
public class MatchLogController {
    private final MatchLogService matchLogService;
    private final AuthSessionService authSessionService;

    public MatchLogController(MatchLogService matchLogService, AuthSessionService authSessionService) {
        this.matchLogService = matchLogService;
        this.authSessionService = authSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchLogRequestResponse create(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token,
            @Valid @RequestBody CreateMatchLogRequest request
    ) {
        Long userId = authSessionService.requireAuthenticatedUserId(token);
        return matchLogService.createRequest(userId, request);
    }

    @GetMapping("/inbox")
    public List<MatchLogRequestResponse> inbox(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token
    ) {
        Long userId = authSessionService.requireAuthenticatedUserId(token);
        return matchLogService.inbox(userId);
    }

    @PostMapping("/{requestId}/decision")
    public MatchLogRequestResponse decide(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token,
            @PathVariable Long requestId,
            @Valid @RequestBody MatchLogDecisionRequest request
    ) {
        Long userId = authSessionService.requireAuthenticatedUserId(token);
        return matchLogService.respond(userId, requestId, request);
    }

    @GetMapping("/history")
    public List<MatchHistoryItemResponse> history(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token
    ) {
        Long userId = authSessionService.requireAuthenticatedUserId(token);
        return matchLogService.history(userId);
    }
}
