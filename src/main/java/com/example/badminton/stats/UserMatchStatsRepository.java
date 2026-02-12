package com.example.badminton.stats;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMatchStatsRepository extends JpaRepository<UserMatchStats, Long> {
    Optional<UserMatchStats> findByUserId(Long userId);
}
