package com.example.badminton.matchlog;

import com.example.badminton.auth.User;
import com.example.badminton.auth.UserRepository;
import com.example.badminton.matchlog.dto.CreateMatchLogRequest;
import com.example.badminton.matchlog.dto.MatchLogDecisionRequest;
import com.example.badminton.matchlog.dto.MatchHistoryItemResponse;
import com.example.badminton.matchlog.dto.MatchLogParticipantResponse;
import com.example.badminton.matchlog.dto.MatchLogRequestResponse;
import com.example.badminton.stats.DashboardStatsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchLogService {
    private final MatchLogRequestRepository matchLogRequestRepository;
    private final MatchLogParticipantRepository matchLogParticipantRepository;
    private final UserRepository userRepository;
    private final DashboardStatsService dashboardStatsService;

    public MatchLogService(
            MatchLogRequestRepository matchLogRequestRepository,
            MatchLogParticipantRepository matchLogParticipantRepository,
            UserRepository userRepository,
            DashboardStatsService dashboardStatsService
    ) {
        this.matchLogRequestRepository = matchLogRequestRepository;
        this.matchLogParticipantRepository = matchLogParticipantRepository;
        this.userRepository = userRepository;
        this.dashboardStatsService = dashboardStatsService;
    }

    @Transactional
    public MatchLogRequestResponse createRequest(Long authenticatedUserId, CreateMatchLogRequest request) {
        MatchFormat format = parseMatchFormat(request.matchFormat());
        TeamSide winnerSide = parseTeamSide(request.winnerSide());
        String matchName = normalizeRequired(request.matchName(), "Match name is required");
        String points = normalizeOptional(request.points());

        List<Long> teamUsers = normalizeUsers(request.teamUserIds());
        List<Long> opponentUsers = normalizeUsers(request.opponentUserIds());

        if (!teamUsers.contains(authenticatedUserId) && !opponentUsers.contains(authenticatedUserId)) {
            teamUsers.add(authenticatedUserId);
        }

        validateTeams(format, teamUsers, opponentUsers);

        Set<Long> participantIds = new LinkedHashSet<>();
        participantIds.addAll(teamUsers);
        participantIds.addAll(opponentUsers);

        Map<Long, User> usersById = userRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (usersById.size() != participantIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected players do not exist");
        }

        MatchLogRequest logRequest = new MatchLogRequest();
        logRequest.setCreatedByUserId(authenticatedUserId);
        logRequest.setMatchName(matchName);
        logRequest.setMatchFormat(format.name());
        logRequest.setWinnerSide(winnerSide.name());
        logRequest.setPoints(points);
        logRequest.setStatus(MatchLogStatus.PENDING.name());
        MatchLogRequest savedRequest = matchLogRequestRepository.save(logRequest);

        List<MatchLogParticipant> participants = new ArrayList<>();
        Instant now = Instant.now();

        for (Long userId : teamUsers) {
            participants.add(buildParticipant(
                    savedRequest.getId(),
                    userId,
                    TeamSide.TEAM,
                    winnerSide,
                    authenticatedUserId,
                    now
            ));
        }
        for (Long userId : opponentUsers) {
            participants.add(buildParticipant(
                    savedRequest.getId(),
                    userId,
                    TeamSide.OPPONENT,
                    winnerSide,
                    authenticatedUserId,
                    now
            ));
        }
        matchLogParticipantRepository.saveAll(participants);

        return toResponse(savedRequest, participants, authenticatedUserId, usersById);
    }

    @Transactional(readOnly = true)
    public List<MatchLogRequestResponse> inbox(Long authenticatedUserId) {
        List<MatchLogParticipant> userRows = matchLogParticipantRepository.findByUserIdOrderByCreatedAtDesc(authenticatedUserId);
        if (userRows.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = userRows.stream()
                .map(MatchLogParticipant::getRequestId)
                .distinct()
                .toList();

        Map<Long, MatchLogRequest> requestsById = matchLogRequestRepository.findAllById(requestIds).stream()
                .collect(Collectors.toMap(MatchLogRequest::getId, Function.identity()));

        List<MatchLogParticipant> allParticipants = matchLogParticipantRepository.findByRequestIdIn(requestIds);
        Map<Long, List<MatchLogParticipant>> participantsByRequest = allParticipants.stream()
                .collect(Collectors.groupingBy(MatchLogParticipant::getRequestId));

        Set<Long> userIds = new LinkedHashSet<>();
        for (MatchLogParticipant participant : allParticipants) {
            userIds.add(participant.getUserId());
        }
        for (MatchLogRequest request : requestsById.values()) {
            userIds.add(request.getCreatedByUserId());
        }

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requestIds.stream()
                .map(requestsById::get)
                .filter(request -> request != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(request -> toResponse(
                        request,
                        participantsByRequest.getOrDefault(request.getId(), List.of()),
                        authenticatedUserId,
                        usersById
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchHistoryItemResponse> history(Long authenticatedUserId) {
        List<MatchLogParticipant> userRows = matchLogParticipantRepository.findByUserIdOrderByCreatedAtDesc(authenticatedUserId);
        if (userRows.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = userRows.stream()
                .map(MatchLogParticipant::getRequestId)
                .distinct()
                .toList();

        Map<Long, MatchLogRequest> requestsById = matchLogRequestRepository.findAllById(requestIds).stream()
                .filter(request -> MatchLogStatus.APPROVED.name().equals(request.getStatus()))
                .collect(Collectors.toMap(MatchLogRequest::getId, Function.identity()));
        if (requestsById.isEmpty()) {
            return List.of();
        }

        List<MatchLogParticipant> allParticipants = matchLogParticipantRepository.findByRequestIdIn(requestsById.keySet());
        Map<Long, List<MatchLogParticipant>> participantsByRequest = allParticipants.stream()
                .collect(Collectors.groupingBy(MatchLogParticipant::getRequestId));

        Set<Long> userIds = new LinkedHashSet<>();
        for (MatchLogParticipant participant : allParticipants) {
            userIds.add(participant.getUserId());
        }
        for (MatchLogRequest request : requestsById.values()) {
            userIds.add(request.getCreatedByUserId());
        }

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return requestsById.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(request -> toHistoryItem(
                        request,
                        participantsByRequest.getOrDefault(request.getId(), List.of()),
                        authenticatedUserId,
                        usersById
                ))
                .filter(item -> item != null)
                .toList();
    }

    @Transactional
    public MatchLogRequestResponse respond(
            Long authenticatedUserId,
            Long requestId,
            MatchLogDecisionRequest decisionRequest
    ) {
        MatchLogRequest request = matchLogRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match log request not found"));

        MatchLogParticipant participant = matchLogParticipantRepository.findByRequestIdAndUserId(requestId, authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this match"));

        MatchLogDecision decision = parseDecision(decisionRequest.decision());
        MatchLogStatus status = MatchLogStatus.valueOf(request.getStatus());
        TeamSide losingSide = losingSideFor(request);
        boolean isSingles = MatchFormat.SINGLES.name().equalsIgnoreCase(request.getMatchFormat());
        boolean participantOnLosingSide = participant.getTeamSide().equals(losingSide.name());
        boolean participantCanRespond = participantOnLosingSide || isSingles;

        if (!participantCanRespond) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only losing team can approve or reject");
        }

        // If everything is already accepted in singles, allow the winner to finalize.
        if (status == MatchLogStatus.PENDING && allParticipantsAccepted(requestId)) {
            request.setStatus(MatchLogStatus.APPROVED.name());
            matchLogRequestRepository.save(request);
            applyApprovedResult(request);
            List<MatchLogParticipant> participants = matchLogParticipantRepository.findByRequestId(requestId);
            Set<Long> userIds = collectUserIds(participants, List.of(request));
            Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            return toResponse(request, participants, authenticatedUserId, usersById);
        }

        if (!MatchLogDecision.PENDING.name().equals(participant.getDecision())) {
            // For singles, winner can "confirm" with ACCEPT even if already accepted; otherwise block.
            if (!(isSingles && decision == MatchLogDecision.ACCEPTED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Decision already submitted");
            }
        }

        if (status == MatchLogStatus.PENDING && decision != MatchLogDecision.PENDING) {
            participant.setDecision(decision.name());
            participant.setRespondedAt(Instant.now());
            matchLogParticipantRepository.save(participant);

            if (decision == MatchLogDecision.REJECTED) {
                request.setStatus(MatchLogStatus.REJECTED.name());
                matchLogRequestRepository.save(request);
            } else if (allParticipantsAccepted(requestId)) {
                request.setStatus(MatchLogStatus.APPROVED.name());
                matchLogRequestRepository.save(request);
                applyApprovedResult(request);
            }
        }

        List<MatchLogParticipant> participants = matchLogParticipantRepository.findByRequestId(requestId);
        Set<Long> userIds = collectUserIds(participants, List.of(request));
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return toResponse(request, participants, authenticatedUserId, usersById);
    }

    private MatchLogParticipant buildParticipant(
            Long requestId,
            Long userId,
            TeamSide teamSide,
            TeamSide winnerSide,
            Long authenticatedUserId,
            Instant now
    ) {
        MatchLogParticipant participant = new MatchLogParticipant();
        participant.setRequestId(requestId);
        participant.setUserId(userId);
        participant.setTeamSide(teamSide.name());

        if (teamSide == winnerSide || userId.equals(authenticatedUserId)) {
            participant.setDecision(MatchLogDecision.ACCEPTED.name());
            participant.setRespondedAt(now);
        } else {
            participant.setDecision(MatchLogDecision.PENDING.name());
        }
        return participant;
    }

    private void validateTeams(MatchFormat format, List<Long> teamUsers, List<Long> opponentUsers) {
        Set<Long> overlap = new LinkedHashSet<>(teamUsers);
        overlap.retainAll(opponentUsers);
        if (!overlap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A player cannot be in both teams");
        }

        if (format == MatchFormat.SINGLES) {
            if (teamUsers.size() != 1 || opponentUsers.size() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Singles requires exactly 1 player per team");
            }
            return;
        }

        if (teamUsers.size() != 2 || opponentUsers.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doubles requires exactly 2 players per team");
        }
    }

    private MatchLogRequestResponse toResponse(
            MatchLogRequest request,
            List<MatchLogParticipant> participants,
            Long viewerId,
            Map<Long, User> usersById
    ) {
        List<MatchLogParticipantResponse> participantResponses = participants.stream()
                .map(participant -> new MatchLogParticipantResponse(
                        participant.getUserId(),
                        usernameFor(participant.getUserId(), usersById),
                        participant.getTeamSide(),
                        participant.getDecision()
                ))
                .toList();

        TeamSide losingSide = losingSideFor(request);
        boolean isSingles = MatchFormat.SINGLES.name().equalsIgnoreCase(request.getMatchFormat());
        boolean canRespond = MatchLogStatus.PENDING.name().equals(request.getStatus())
                && participants.stream()
                .anyMatch(participant -> {
                    boolean isViewer = participant.getUserId().equals(viewerId);
                    boolean isPending = MatchLogDecision.PENDING.name().equals(participant.getDecision());
                    boolean isOnLosingSide = participant.getTeamSide().equals(losingSide.name());
                    boolean isWinnerSide = participant.getTeamSide().equals(request.getWinnerSide());
                    boolean singlesWinnerCanRespond = isSingles && isWinnerSide;
                    return isViewer && ((isPending && (isOnLosingSide || isSingles)) || singlesWinnerCanRespond);
                });

        return new MatchLogRequestResponse(
                request.getId(),
                request.getMatchName(),
                request.getMatchFormat(),
                request.getWinnerSide(),
                request.getPoints(),
                request.getStatus(),
                request.getCreatedByUserId(),
                usernameFor(request.getCreatedByUserId(), usersById),
                request.getCreatedAt(),
                canRespond,
                participantResponses
        );
    }

    private boolean allParticipantsAccepted(Long requestId) {
        return matchLogParticipantRepository.findByRequestId(requestId).stream()
                .allMatch(participant -> MatchLogDecision.ACCEPTED.name().equals(participant.getDecision()));
    }

    private TeamSide losingSideFor(MatchLogRequest request) {
        TeamSide winner = TeamSide.valueOf(request.getWinnerSide());
        return winner == TeamSide.TEAM ? TeamSide.OPPONENT : TeamSide.TEAM;
    }

    private void applyApprovedResult(MatchLogRequest request) {
        TeamSide winner = TeamSide.valueOf(request.getWinnerSide());
        List<MatchLogParticipant> participants = matchLogParticipantRepository.findByRequestId(request.getId());
        for (MatchLogParticipant participant : participants) {
            TeamSide side = TeamSide.valueOf(participant.getTeamSide());
            boolean won = side == winner;
            dashboardStatsService.recordMatchOutcome(participant.getUserId(), won);
        }
    }

    private Set<Long> collectUserIds(List<MatchLogParticipant> participants, Collection<MatchLogRequest> requests) {
        Set<Long> ids = new LinkedHashSet<>();
        for (MatchLogParticipant participant : participants) {
            ids.add(participant.getUserId());
        }
        for (MatchLogRequest request : requests) {
            ids.add(request.getCreatedByUserId());
        }
        return ids;
    }

    private MatchHistoryItemResponse toHistoryItem(
            MatchLogRequest request,
            List<MatchLogParticipant> participants,
            Long viewerId,
            Map<Long, User> usersById
    ) {
        MatchLogParticipant myRow = participants.stream()
                .filter(p -> p.getUserId().equals(viewerId))
                .findFirst()
                .orElse(null);
        if (myRow == null) {
            return null;
        }

        boolean userWon = myRow.getTeamSide().equals(request.getWinnerSide());
        List<String> teamUsernames = participants.stream()
                .filter(p -> p.getTeamSide().equals(TeamSide.TEAM.name()))
                .map(p -> usernameFor(p.getUserId(), usersById))
                .toList();
        List<String> opponentUsernames = participants.stream()
                .filter(p -> p.getTeamSide().equals(TeamSide.OPPONENT.name()))
                .map(p -> usernameFor(p.getUserId(), usersById))
                .toList();

        return new MatchHistoryItemResponse(
                request.getId(),
                request.getMatchName(),
                request.getMatchFormat(),
                request.getPoints(),
                request.getWinnerSide(),
                usernameFor(request.getCreatedByUserId(), usersById),
                request.getCreatedAt(),
                userWon,
                myRow.getTeamSide(),
                teamUsernames,
                opponentUsernames
        );
    }

    private List<Long> normalizeUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(new LinkedHashSet<>(userIds.stream()
                .filter(id -> id != null && id > 0)
                .toList()));
    }

    private MatchFormat parseMatchFormat(String value) {
        try {
            return MatchFormat.valueOf(normalizeRequired(value, "Match format is required").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid match format");
        }
    }

    private TeamSide parseTeamSide(String value) {
        try {
            return TeamSide.valueOf(normalizeRequired(value, "Winner side is required").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid winner side");
        }
    }

    private MatchLogDecision parseDecision(String value) {
        String normalized = normalizeRequired(value, "Decision is required").toUpperCase();
        return switch (normalized) {
            case "ACCEPT", "ACCEPTED" -> MatchLogDecision.ACCEPTED;
            case "REJECT", "REJECTED" -> MatchLogDecision.REJECTED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid decision");
        };
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String usernameFor(Long userId, Map<Long, User> usersById) {
        User user = usersById.get(userId);
        if (user == null) {
            return "player-" + userId;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }
}
