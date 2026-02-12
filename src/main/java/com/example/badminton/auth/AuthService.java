package com.example.badminton.auth;

import com.example.badminton.auth.dto.AuthResponse;
import com.example.badminton.auth.dto.LoginRequest;
import com.example.badminton.auth.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final String LOGIN_FAILURE_MESSAGE = "Account doesnot exist Or Password incorrect error";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email id already in use");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        String normalized = identifier.toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByUsername(normalized))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, LOGIN_FAILURE_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, LOGIN_FAILURE_MESSAGE);
        }

        return new AuthResponse(user.getId(), user.getEmail(), user.getUsername());
    }
}
