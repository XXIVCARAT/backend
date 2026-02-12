package com.example.badminton.stats;

import com.example.badminton.auth.UserRepository;
import com.example.badminton.stats.dto.DashboardStatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardStatsService {
    private static final int DEFAULT_MATCHES_WON = 16;
    private static final int DEFAULT_MATCHES_LOST = 8;

    private final UserRepository userRepository;
    private final UserMatchStatsRepository userMatchStatsRepository;

    public DashboardStatsService(UserRepository userRepository, UserMatchStatsRepository userMatchStatsRepository) {
        this.userRepository = userRepository;
        this.userMatchStatsRepository = userMatchStatsRepository;
    }

    @Transactional
    public void initializeForUser(Long userId) {
        userMatchStatsRepository.findByUserId(userId).orElseGet(() -> {
            UserMatchStats stats = new UserMatchStats();
            stats.setUserId(userId);
            stats.setMatchesWon(DEFAULT_MATCHES_WON);
            stats.setMatchesLost(DEFAULT_MATCHES_LOST);
            return userMatchStatsRepository.save(stats);
        });
    }

    @Transactional
    public DashboardStatsResponse getDashboardStats(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserMatchStats stats = userMatchStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserMatchStats created = new UserMatchStats();
                    created.setUserId(userId);
                    created.setMatchesWon(DEFAULT_MATCHES_WON);
                    created.setMatchesLost(DEFAULT_MATCHES_LOST);
                    return userMatchStatsRepository.save(created);
                });

        int matchesWon = Math.max(stats.getMatchesWon(), 0);
        int matchesLost = Math.max(stats.getMatchesLost(), 0);
        int matchesPlayed = matchesWon + matchesLost;
        int winRate = matchesPlayed == 0 ? 0 : (int) Math.round((matchesWon * 100.0) / matchesPlayed);
        int rating = calculateRating(matchesPlayed, winRate);
        int rank = calculateRank(rating);

        return new DashboardStatsResponse(
                userId,
                rank,
                rating,
                matchesPlayed,
                matchesWon,
                matchesLost,
                winRate,
                calculateTier(rating)
        );
    }

    private int calculateRating(int matchesPlayed, int winRate) {
        int rawRating = 1020 + (matchesPlayed * 15) + (winRate * 7);
        int roundedToTens = (int) (Math.round(rawRating / 10.0) * 10);
        return Math.max(800, roundedToTens);
    }

    private int calculateRank(int rating) {
        return Math.max(1, 20 - (rating / 115));
    }

    private String calculateTier(int rating) {
        if (rating >= 2100) return "MASTER";
        if (rating >= 1800) return "DIAMOND";
        if (rating >= 1600) return "PLATINUM";
        if (rating >= 1400) return "GOLD";
        if (rating >= 1200) return "SILVER";
        return "BRONZE";
    }
}
