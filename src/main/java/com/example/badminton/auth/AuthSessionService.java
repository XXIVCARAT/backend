package com.example.badminton.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthSessionService {
    private final JwtService jwtService;

    public AuthSessionService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Long requireAuthenticatedUserId(String token) {
        return jwtService.parseUserId(token == null ? "" : token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
