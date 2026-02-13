package com.example.badminton.stats;

import com.example.badminton.auth.User;
import com.example.badminton.auth.UserRepository;
import com.example.badminton.stats.dto.LeaderboardEntryResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderboardService {
    private final UserMatchStatsRepository userMatchStatsRepository;
    private final UserRepository userRepository;

    public LeaderboardService(UserMatchStatsRepository userMatchStatsRepository, UserRepository userRepository) {
        this.userMatchStatsRepository = userMatchStatsRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard() {
        var stats = userMatchStatsRepository.findAllOrdered();
        if (stats.isEmpty()) {
            return List.of();
        }

        Map<Long, User> usersById = userRepository.findAllById(
                        stats.stream().map(s -> s.getUserId()).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardEntryResponse> rows = new ArrayList<>();
        int rank = 1;
        for (var s : stats) {
            int wins = Math.max(0, s.getMatchesWon());
            int losses = Math.max(0, s.getMatchesLost());
            int played = wins + losses;
            int winRate = played == 0 ? 0 : (int) Math.round((wins * 100.0) / played);
            int rating = s.getRating() == null ? 1000 : s.getRating();
            String username = resolveUsername(usersById.get(s.getUserId()));
            rows.add(new LeaderboardEntryResponse(
                    rank++,
                    s.getUserId(),
                    username,
                    rating,
                    wins,
                    losses,
                    winRate,
                    calculateTier(rating)
            ));
        }
        return rows;
    }

    private String resolveUsername(User user) {
        if (user == null) return "player";
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private String calculateTier(int rating) {
        if (rating >= 2100) return "DIAMOND";
        if (rating >= 1800) return "PLATINUM";
        if (rating >= 1600) return "GOLD";
        if (rating >= 1400) return "SILVER";
        if (rating >= 1200) return "BRONZE";
        return "BRONZE";
    }
}
