package com.example.badminton.players;

import com.example.badminton.auth.AuthConstants;
import com.example.badminton.auth.AuthSessionService;
import com.example.badminton.auth.UserRepository;
import com.example.badminton.players.dto.PlayerOptionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlayerController {
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;

    public PlayerController(UserRepository userRepository, AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
    }

    @GetMapping("/players")
    public List<PlayerOptionResponse> players(
            @CookieValue(name = AuthConstants.AUTH_COOKIE_NAME, required = false) String token
    ) {
        authSessionService.requireAuthenticatedUserId(token);

        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(user -> new PlayerOptionResponse(user.getId(), user.getUsername()))
                .toList();
    }
}
