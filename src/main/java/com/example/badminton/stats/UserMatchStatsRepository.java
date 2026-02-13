package com.example.badminton.stats;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMatchStatsRepository extends JpaRepository<UserMatchStats, Long> {
    Optional<UserMatchStats> findByUserId(Long userId);

    @Query("""
            select count(s) from UserMatchStats s
            where s.rating > :rating
               or (s.rating = :rating and s.userId < :userId)
            """)
    long countBetterRanked(@Param("rating") int rating, @Param("userId") Long userId);

    @Query("select s from UserMatchStats s order by s.rating desc, s.userId asc")
    List<UserMatchStats> findAllOrdered();
}
