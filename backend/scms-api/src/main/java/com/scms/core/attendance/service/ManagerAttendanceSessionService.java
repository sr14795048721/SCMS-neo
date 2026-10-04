package com.scms.core.attendance.service;

import com.scms.core.attendance.domain.AttendanceRecordEntity;
import com.scms.core.attendance.domain.AttendanceRecordStatus;
import com.scms.core.attendance.domain.AttendanceSessionEntity;
import com.scms.core.attendance.domain.AttendanceSessionStatus;
import com.scms.core.attendance.dto.ManagerAttendanceSessionCreateRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionDetailResponse;
import com.scms.core.attendance.dto.ManagerAttendanceSessionMemberResponse;
import com.scms.core.attendance.dto.ManagerAttendanceSessionSummaryResponse;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.attendance.repository.AttendanceSessionRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.score.domain.ScoreRecordEntity;
import com.scms.core.score.domain.ScoreRuleEntity;
import com.scms.core.score.domain.ScoreRuleScope;
import com.scms.core.score.domain.ScoreRuleStatus;
import com.scms.core.score.repository.ScoreRecordRepository;
import com.scms.core.score.repository.ScoreRuleRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerAttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ScoreRuleRepository scoreRuleRepository;
    private final ScoreRecordRepository scoreRecordRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final AttendanceSignatureStorageService signatureStorageService;

    public ManagerAttendanceSessionService(AttendanceSessionRepository attendanceSessionRepository,
                                           AttendanceRecordRepository attendanceRecordRepository,
                                           ClubRepository clubRepository,
                                           ClubManagerBindingRepository clubManagerBindingRepository,
                                           ClubStudentMemberRepository clubStudentMemberRepository,
                                           ScoreRuleRepository scoreRuleRepository,
                                           ScoreRecordRepository scoreRecordRepository,
                                           StudentInfoRepository studentInfoRepository,
                                           UserRepository userRepository,
                                           CurrentUserProvider currentUserProvider,
                                           AuditService auditService,
                                           AttendanceSignatureStorageService signatureStorageService) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.scoreRuleRepository = scoreRuleRepository;
        this.scoreRecordRepository = scoreRecordRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.signatureStorageService = signatureStorageService;
    }

    @Transactional(readOnly = true)
    public List<ManagerAttendanceSessionSummaryResponse> listSessions(Long clubId) {
        requireManagerAccessToClub(clubId);
        List<AttendanceSessionEntity> sessions = attendanceSessionRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId);
        return buildSummaries(sessions);
    }

    @Transactional(readOnly = true)
    public ManagerAttendanceSessionDetailResponse getSession(Long clubId, Long sessionId) {
        requireManagerAccessToClub(clubId);
        AttendanceSessionEntity session = getRequiredSession(clubId, sessionId);
        return buildDetail(session);
    }

    @Transactional
    public ManagerAttendanceSessionDetailResponse createSession(Long clubId, ManagerAttendanceSessionCreateRequest request) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubEntity club = getRequiredClub(clubId);
        ScoreRuleEntity scoreRule = getValidScoreRuleForClub(clubId, request.scoreRuleId());

        AttendanceSessionEntity session = new AttendanceSessionEntity();
        session.setClubId(club.getId());
        session.setTitle(request.title().trim());
        session.setScoreRuleId(scoreRule.getId());
        session.setCreatedBy(manager.userId());
        session.setStatus(AttendanceSessionStatus.OPEN);
        session.setStartedAt(Instant.now());
        session.setShareToken(generateShareToken());
        AttendanceSessionEntity savedSession = attendanceSessionRepository.save(session);

        List<ClubStudentMemberEntity> members = clubStudentMemberRepository.findAllByClubId(clubId);
        List<AttendanceRecordEntity> records = members.stream()
                .map(member -> {
                    AttendanceRecordEntity record = new AttendanceRecordEntity();
                    record.setSessionId(savedSession.getId());
                    record.setStudentUserId(member.getStudentUserId());
                    record.setStatus(AttendanceRecordStatus.PENDING);
                    record.setSettled(false);
                    return record;
                })
                .toList();
        attendanceRecordRepository.saveAll(records);

        auditService.create(
                "MANAGER_ATTENDANCE_SESSION_CREATED",
                manager.userId(),
                "ATTENDANCE_SESSION",
                String.valueOf(savedSession.getId()),
                "Club manager created an attendance session for " + club.getName()
        );
        return buildDetail(savedSession);
    }

    @Transactional
    public ManagerAttendanceSessionDetailResponse settle(Long clubId, Long sessionId) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        AttendanceSessionEntity session = getRequiredSession(clubId, sessionId);
        ScoreRuleEntity scoreRule = getValidScoreRuleForClub(clubId, session.getScoreRuleId());

        List<AttendanceRecordEntity> records = attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(sessionId);
        scoreRecordRepository.deleteAllByAttendanceSessionId(sessionId);

        Instant now = Instant.now();
        for (AttendanceRecordEntity record : records) {
            // Signed check-in (or historical check-out) counts; unmarked students get nothing.
            boolean shouldSettle = record.getCheckInAt() != null
                    && (record.getStatus() == AttendanceRecordStatus.CHECKED_IN
                    || record.getStatus() == AttendanceRecordStatus.CHECKED_OUT);
            record.setSettled(shouldSettle);
            if (shouldSettle) {
                ScoreRecordEntity scoreRecord = new ScoreRecordEntity();
                scoreRecord.setClubId(clubId);
                scoreRecord.setUserId(record.getStudentUserId());
                scoreRecord.setRuleId(scoreRule.getId());
                scoreRecord.setAttendanceSessionId(sessionId);
                scoreRecord.setScoreDelta(scoreRule.getScoreDelta());
                scoreRecord.setReason("社团课签到：" + session.getTitle());
                scoreRecord.setOperatorUserId(manager.userId());
                scoreRecordRepository.save(scoreRecord);
            }
        }
        attendanceRecordRepository.saveAll(records);

        session.setStatus(AttendanceSessionStatus.COMPLETED);
        session.setEndedAt(now);
        session.setSettledAt(now);
        AttendanceSessionEntity savedSession = attendanceSessionRepository.save(session);

        auditService.create(
                "MANAGER_ATTENDANCE_SESSION_SETTLED",
                manager.userId(),
                "ATTENDANCE_SESSION",
                String.valueOf(sessionId),
                "Club manager settled attendance session scores"
        );
        return buildDetail(savedSession);
    }

    @Transactional(readOnly = true)
    public Path resolveSignature(Long clubId, Long sessionId, Long studentUserId) {
        requireManagerAccessToClub(clubId);
        AttendanceSessionEntity session = getRequiredSession(clubId, sessionId);
        AttendanceRecordEntity record = attendanceRecordRepository
                .findBySessionIdAndStudentUserId(session.getId(), studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "attendance record not found"));
        if (record.getSignaturePath() == null || record.getSignaturePath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "attendance signature not found");
        }
        return signatureStorageService.resolveContent(record.getSignaturePath());
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ManagerAttendanceSessionDetailResponse buildDetail(AttendanceSessionEntity session) {
        ManagerAttendanceSessionSummaryResponse summary = buildSummaries(List.of(session)).get(0);
        List<AttendanceRecordEntity> records = attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        Set<Long> studentIds = records.stream().map(AttendanceRecordEntity::getStudentUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserEntity> userMap = loadStudentUserMap(studentIds);
        Map<Long, StudentInfoEntity> infoMap = loadStudentInfoMap(studentIds);

        List<ManagerAttendanceSessionMemberResponse> members = records.stream()
                .sorted(Comparator
                        .comparing(
                                (AttendanceRecordEntity item) -> trimToNull(resolveStudentDisplayName(item.getStudentUserId(), userMap, infoMap)),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(AttendanceRecordEntity::getStudentUserId))
                .map(record -> {
                    StudentInfoEntity info = infoMap.get(record.getStudentUserId());
                    return new ManagerAttendanceSessionMemberResponse(
                            record.getStudentUserId(),
                            resolveStudentDisplayName(record.getStudentUserId(), userMap, infoMap),
                            info == null ? "" : defaultString(info.getGrade()),
                            info == null ? "" : defaultString(info.getClassName()),
                            record.getStatus().name(),
                            record.getSignedName(),
                            record.getSignedRole(),
                            record.getSignaturePath(),
                            record.getCheckInAt(),
                            record.getCheckOutAt(),
                            record.isSettled()
                    );
                })
                .toList();
        return new ManagerAttendanceSessionDetailResponse(summary, members);
    }

    private List<ManagerAttendanceSessionSummaryResponse> buildSummaries(List<AttendanceSessionEntity> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream().map(AttendanceSessionEntity::getId).toList();
        List<AttendanceRecordEntity> records = attendanceRecordRepository.findAllBySessionIdIn(sessionIds);
        Map<Long, List<AttendanceRecordEntity>> recordMap = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecordEntity::getSessionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, ScoreRuleEntity> ruleMap = scoreRuleRepository.findAllById(
                        sessions.stream().map(AttendanceSessionEntity::getScoreRuleId).filter(Objects::nonNull).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ScoreRuleEntity::getId, item -> item));

        return sessions.stream()
                .map(session -> {
                    List<AttendanceRecordEntity> sessionRecords = recordMap.getOrDefault(session.getId(), List.of());
                    long checkedInCount = sessionRecords.stream().filter(item -> item.getStatus() == AttendanceRecordStatus.CHECKED_IN).count();
                    long checkedOutCount = sessionRecords.stream().filter(item -> item.getStatus() == AttendanceRecordStatus.CHECKED_OUT).count();
                    long settledCount = sessionRecords.stream().filter(AttendanceRecordEntity::isSettled).count();
                    ScoreRuleEntity rule = ruleMap.get(session.getScoreRuleId());
                    return new ManagerAttendanceSessionSummaryResponse(
                            session.getId(),
                            session.getClubId(),
                            defaultString(session.getTitle()),
                            session.getScoreRuleId(),
                            rule == null ? "" : defaultString(rule.getName()),
                            rule == null ? 0 : rule.getScoreDelta(),
                            session.getStatus().name(),
                            session.getShareToken(),
                            sessionRecords.size(),
                            (int) checkedInCount,
                            (int) checkedOutCount,
                            (int) settledCount,
                            session.getStartedAt(),
                            session.getEndedAt(),
                            session.getSettledAt(),
                            session.getCreatedAt(),
                            session.getUpdatedAt()
                    );
                })
                .toList();
    }

    private AttendanceSessionEntity getRequiredSession(Long clubId, Long sessionId) {
        return attendanceSessionRepository.findByIdAndClubId(sessionId, clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "attendance session not found"));
    }

    private ScoreRuleEntity getValidScoreRuleForClub(Long clubId, Long scoreRuleId) {
        ScoreRuleEntity rule = scoreRuleRepository.findById(scoreRuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "score rule not found"));
        if (rule.getStatus() != ScoreRuleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "score rule is inactive");
        }
        if (rule.getScopeType() == ScoreRuleScope.CLUB && !Objects.equals(rule.getClubId(), clubId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "score rule does not belong to the club");
        }
        if (rule.getScopeType() != ScoreRuleScope.GLOBAL && rule.getScopeType() != ScoreRuleScope.CLUB) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "score rule is unavailable for attendance");
        }
        return rule;
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private AuthenticatedUser requireManagerAccessToClub(Long clubId) {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager role required");
        }
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, currentUser.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot access this club");
        }
        return currentUser;
    }

    private Map<Long, UserEntity> loadStudentUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdInAndRole(userIds, UserRole.STUDENT)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, item -> item));
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, StudentInfoEntity> map = new LinkedHashMap<>();
        for (StudentInfoEntity item : studentInfoRepository.findAllByUserIdIn(userIds)) {
            StudentInfoEntity existing = map.get(item.getUserId());
            if (existing == null || isStudentInfoNewer(item, existing)) {
                map.put(item.getUserId(), item);
            }
        }
        return map;
    }

    private String resolveStudentDisplayName(Long userId,
                                             Map<Long, UserEntity> userMap,
                                             Map<Long, StudentInfoEntity> infoMap) {
        StudentInfoEntity info = infoMap.get(userId);
        String displayName = info == null ? null : trimToNull(info.getDisplayName());
        return displayName == null ? "" : displayName;
    }

    private boolean isStudentInfoNewer(StudentInfoEntity candidate, StudentInfoEntity current) {
        if (candidate.getUpdatedAt() != null && current.getUpdatedAt() != null) {
            return candidate.getUpdatedAt().isAfter(current.getUpdatedAt());
        }
        if (candidate.getUpdatedAt() != null) {
            return true;
        }
        if (current.getUpdatedAt() != null) {
            return false;
        }
        return candidate.getId() != null && current.getId() != null && candidate.getId() > current.getId();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
