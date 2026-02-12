package com.example.badminton.matchlog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchLogParticipantRepository extends JpaRepository<MatchLogParticipant, Long> {
    List<MatchLogParticipant> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<MatchLogParticipant> findByRequestIdIn(Collection<Long> requestIds);
    List<MatchLogParticipant> findByRequestId(Long requestId);
    Optional<MatchLogParticipant> findByRequestIdAndUserId(Long requestId, Long userId);
}
