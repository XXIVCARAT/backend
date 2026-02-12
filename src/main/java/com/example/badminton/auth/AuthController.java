package com.example.badminton.auth;

import com.example.badminton.auth.dto.AuthResponse;
import com.example.badminton.auth.dto.LoginRequest;
import com.example.badminton.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletResponse response, HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.login(request);
        String token = jwtService.createToken(authResponse.id());
        response.addHeader(HttpHeaders.SET_COOKIE, buildAuthCookie(token, httpRequest).toString());
        return authResponse;
    }

    @GetMapping("/auth/me")
    public AuthResponse me(@CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token) {
        Long userId = jwtService.parseUserId(token == null ? "" : token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        return authService.getById(userId);
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(jakarta.servlet.http.HttpServletResponse response, HttpServletRequest httpRequest) {
        response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie(httpRequest).toString());
        return Map.of("message", "Logged out");
    }

    private ResponseCookie buildAuthCookie(String token, HttpServletRequest request) {
        boolean secure = shouldUseSecureCookie(request);

        return ResponseCookie.from(AuthConstants.AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.getTokenTtlSeconds()))
                .build();
    }

    private ResponseCookie clearAuthCookie(HttpServletRequest request) {
        boolean secure = shouldUseSecureCookie(request);

        return ResponseCookie.from(AuthConstants.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return "https".equalsIgnoreCase(forwardedProto);
        }

        String host = request.getServerName();
        return host != null
                && !"localhost".equalsIgnoreCase(host)
                && !"127.0.0.1".equals(host)
                && !"::1".equals(host);
    }
}
