package com.example.badminton.stats;

import com.example.badminton.auth.UserRepository;
import com.example.badminton.stats.dto.DashboardStatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardStatsService {
    private static final int DEFAULT_MATCHES_WON = 0;
    private static final int DEFAULT_MATCHES_LOST = 0;
    private static final int DEFAULT_RATING = 1000;

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
            stats.setRating(DEFAULT_RATING);
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
                    created.setRating(DEFAULT_RATING);
                    return userMatchStatsRepository.save(created);
                });

        boolean changed = false;
        int matchesWon = Math.max(stats.getMatchesWon(), 0);
        if (matchesWon != stats.getMatchesWon()) {
            stats.setMatchesWon(matchesWon);
            changed = true;
        }

        int matchesLost = Math.max(stats.getMatchesLost(), 0);
        if (matchesLost != stats.getMatchesLost()) {
            stats.setMatchesLost(matchesLost);
            changed = true;
        }

        int rating = normalizeRating(stats.getRating());
        if (stats.getRating() == null || stats.getRating() != rating) {
            stats.setRating(rating);
            changed = true;
        }

        if (changed) {
            stats = userMatchStatsRepository.save(stats);
        }

        int matchesPlayed = matchesWon + matchesLost;
        int winRate = matchesPlayed == 0 ? 0 : (int) Math.round((matchesWon * 100.0) / matchesPlayed);
        int rank = toRank(userMatchStatsRepository.countBetterRanked(rating, stats.getUserId()) + 1);

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

    private int normalizeRating(Integer rating) {
        if (rating == null || rating < 1) {
            return DEFAULT_RATING;
        }
        return rating;
    }

    private int toRank(long rankValue) {
        if (rankValue > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) rankValue);
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
