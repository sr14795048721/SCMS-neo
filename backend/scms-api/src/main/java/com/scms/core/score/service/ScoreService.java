package com.scms.core.score.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.score.domain.ScoreRecordEntity;
import com.scms.core.score.domain.ScoreRuleEntity;
import com.scms.core.score.domain.ScoreRuleScope;
import com.scms.core.score.domain.ScoreRuleStatus;
import com.scms.core.score.dto.AdminScoreRecordMutationRequest;
import com.scms.core.score.dto.AdminScoreRecordResponse;
import com.scms.core.score.dto.AdminScoreRuleMutationRequest;
import com.scms.core.score.dto.ClubScoreRankingResponse;
import com.scms.core.score.dto.ClubScoreRecordMutationRequest;
import com.scms.core.score.dto.ClubScoreRecordResponse;
import com.scms.core.score.dto.ScoreRuleResponse;
import com.scms.core.score.dto.ScoreSummaryClubResponse;
import com.scms.core.score.dto.ScoreSummaryResponse;
import com.scms.core.score.dto.SchoolScoreRankingResponse;
import com.scms.core.score.dto.StudentScoreRecordResponse;
import com.scms.core.score.repository.ClubScoreTotalProjection;
import com.scms.core.score.repository.ScoreRecordRepository;
import com.scms.core.score.repository.ScoreRuleRepository;
import com.scms.core.score.repository.UserScoreTotalProjection;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final ScoreRuleRepository scoreRuleRepository;
    private final ScoreRecordRepository scoreRecordRepository;
    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final ScoreBalanceService scoreBalanceService;

    public ScoreService(ScoreRuleRepository scoreRuleRepository,
                        ScoreRecordRepository scoreRecordRepository,
                        ClubRepository clubRepository,
                        ClubStudentMemberRepository clubStudentMemberRepository,
                        ClubManagerBindingRepository clubManagerBindingRepository,
                        UserRepository userRepository,
                        StudentInfoRepository studentInfoRepository,
                        CurrentUserProvider currentUserProvider,
                        AuditService auditService,
                        ScoreBalanceService scoreBalanceService) {
        this.scoreRuleRepository = scoreRuleRepository;
        this.scoreRecordRepository = scoreRecordRepository;
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.scoreBalanceService = scoreBalanceService;
    }

    @Transactional(readOnly = true)
    public List<ScoreRuleResponse> listAdminRules() {
        requireAdmin();
        return buildRuleResponses(scoreRuleRepository.findAllByOrderByCreatedAtDescIdDesc());
    }

    @Transactional
    public ScoreRuleResponse createRule(AdminScoreRuleMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();

        ScoreRuleEntity entity = new ScoreRuleEntity();
        entity.setCreatedBy(admin.userId());
        applyRuleMutation(entity, request);

        ScoreRuleEntity saved = scoreRuleRepository.save(entity);
        auditService.create("ADMIN_SCORE_RULE_CREATED", admin.userId(), "SCORE_RULE", String.valueOf(saved.getId()), "Administrator created a score rule");
        return buildRuleResponses(List.of(saved)).get(0);
    }

    @Transactional
    public ScoreRuleResponse updateRule(Long ruleId, AdminScoreRuleMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        ScoreRuleEntity entity = getRequiredRule(ruleId);
        applyRuleMutation(entity, request);

        ScoreRuleEntity saved = scoreRuleRepository.save(entity);
        auditService.create("ADMIN_SCORE_RULE_UPDATED", admin.userId(), "SCORE_RULE", String.valueOf(saved.getId()), "Administrator updated a score rule");
        return buildRuleResponses(List.of(saved)).get(0);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        AuthenticatedUser admin = requireAdmin();
        ScoreRuleEntity entity = getRequiredRule(ruleId);
        if (scoreRecordRepository.countByRuleId(ruleId) > 0) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS, "score rule is still referenced by score records");
        }
        scoreRuleRepository.delete(entity);
        auditService.create("ADMIN_SCORE_RULE_DELETED", admin.userId(), "SCORE_RULE", String.valueOf(ruleId), "Administrator deleted a score rule");
    }

    @Transactional(readOnly = true)
    public List<ScoreRuleResponse> listClubRules(Long clubId) {
        requireAdminOrManagerForClub(clubId);
        getRequiredClub(clubId);

        List<ScoreRuleEntity> rules = scoreRuleRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .filter(rule -> rule.getStatus() == ScoreRuleStatus.ACTIVE)
                .filter(rule -> rule.getScopeType() == ScoreRuleScope.GLOBAL
                        || (rule.getScopeType() == ScoreRuleScope.CLUB && Objects.equals(rule.getClubId(), clubId)))
                .sorted(Comparator
                        .comparing((ScoreRuleEntity rule) -> rule.getScopeType() == ScoreRuleScope.GLOBAL ? 0 : 1)
                        .thenComparing(ScoreRuleEntity::getCreatedAt, Comparator.reverseOrder())
                        .thenComparing(ScoreRuleEntity::getId, Comparator.reverseOrder()))
                .toList();
        return buildRuleResponses(rules);
    }

    @Transactional(readOnly = true)
    public List<ClubScoreRecordResponse> listClubScores(Long clubId) {
        requireAdminOrManagerForClub(clubId);
        getRequiredClub(clubId);

        List<ScoreRecordEntity> records = scoreRecordRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId);
        Set<Long> studentIds = records.stream().map(ScoreRecordEntity::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> operatorIds = records.stream().map(ScoreRecordEntity::getOperatorUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> ruleIds = records.stream().map(ScoreRecordEntity::getRuleId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, UserEntity> studentUserMap = loadUserMap(studentIds);
        Map<Long, UserEntity> operatorUserMap = loadUserMap(operatorIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(studentIds);
        Map<Long, ScoreRuleEntity> ruleMap = scoreRuleRepository.findAllById(ruleIds)
                .stream()
                .collect(Collectors.toMap(ScoreRuleEntity::getId, item -> item));

        return records.stream()
                .map(record -> new ClubScoreRecordResponse(
                        record.getId(),
                        record.getUserId(),
                        defaultString(studentUserMap.get(record.getUserId()) != null ? studentUserMap.get(record.getUserId()).getUsername() : ""),
                        resolveStudentDisplayName(record.getUserId(), studentUserMap, studentInfoMap),
                        defaultString(studentInfoMap.get(record.getUserId()) != null ? studentInfoMap.get(record.getUserId()).getStudentNo() : ""),
                        record.getRuleId(),
                        defaultString(ruleMap.get(record.getRuleId()) != null ? ruleMap.get(record.getRuleId()).getName() : ""),
                        record.getScoreDelta(),
                        record.getReason(),
                        record.getOperatorUserId(),
                        defaultString(operatorUserMap.get(record.getOperatorUserId()) != null ? operatorUserMap.get(record.getOperatorUserId()).getUsername() : ""),
                        record.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public ClubScoreRecordResponse addClubScore(Long clubId, ClubScoreRecordMutationRequest request) {
        AuthenticatedUser operator = requireAdminOrManagerForClub(clubId);
        getRequiredClub(clubId);

        Long userId = request.userId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "target user is invalid");
        }
        if (!clubStudentMemberRepository.existsByClubIdAndStudentUserId(clubId, userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "target user is not a club member");
        }

        ScoreRuleEntity rule = null;
        Integer scoreDelta = request.scoreDelta();
        if (request.ruleId() != null) {
            rule = getRequiredRule(request.ruleId());
            if (rule.getStatus() != ScoreRuleStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "score rule is inactive");
            }
            if (rule.getScopeType() == ScoreRuleScope.CLUB && !Objects.equals(rule.getClubId(), clubId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "score rule does not belong to the club");
            }
            scoreDelta = rule.getScoreDelta();
        } else if (!operator.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club manager must select a score rule");
        }

        if (scoreDelta == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score delta is required");
        }

        String reason = trimToNull(request.reason());
        if (reason == null) {
            reason = rule != null ? "Applied rule: " + rule.getName() : "Manual score adjustment";
        }

        ScoreRecordEntity entity = new ScoreRecordEntity();
        entity.setClubId(clubId);
        entity.setUserId(userId);
        entity.setRuleId(rule == null ? null : rule.getId());
        entity.setScoreDelta(scoreDelta);
        entity.setReason(reason);
        entity.setOperatorUserId(operator.userId());
        ScoreRecordEntity saved = scoreRecordRepository.save(entity);

        Map<Long, UserEntity> studentMap = loadUserMap(List.of(userId));
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(List.of(userId));

        return new ClubScoreRecordResponse(
                saved.getId(),
                saved.getUserId(),
                defaultString(studentMap.get(userId) != null ? studentMap.get(userId).getUsername() : ""),
                resolveStudentDisplayName(userId, studentMap, studentInfoMap),
                defaultString(studentInfoMap.get(userId) != null ? studentInfoMap.get(userId).getStudentNo() : ""),
                saved.getRuleId(),
                rule == null ? "" : rule.getName(),
                saved.getScoreDelta(),
                saved.getReason(),
                saved.getOperatorUserId(),
                operator.username(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminScoreRecordResponse> listAdminScores(Long clubId) {
        requireAdmin();
        List<ScoreRecordEntity> records = clubId == null
                ? scoreRecordRepository.findAllByOrderByCreatedAtDescIdDesc()
                : scoreRecordRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId);
        return buildAdminScoreRecords(records);
    }

    @Transactional
    public AdminScoreRecordResponse addAdminScore(AdminScoreRecordMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        ClubEntity club = getRequiredClub(request.clubId());

        Long userId = request.userId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "target user is invalid");
        }
        if (!clubStudentMemberRepository.existsByClubIdAndStudentUserId(club.getId(), userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "target user is not a club member");
        }

        ScoreRuleEntity rule = null;
        Integer scoreDelta = request.scoreDelta();
        if (request.ruleId() != null) {
            rule = getRequiredRule(request.ruleId());
            if (rule.getStatus() != ScoreRuleStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "score rule is inactive");
            }
            if (rule.getScopeType() == ScoreRuleScope.CLUB && !Objects.equals(rule.getClubId(), club.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "score rule does not belong to the club");
            }
            scoreDelta = rule.getScoreDelta();
        }
        if (scoreDelta == null || scoreDelta == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score delta must not be zero");
        }

        String reason = trimToNull(request.reason());
        if (reason == null) {
            reason = rule != null ? "Applied rule: " + rule.getName() : "Manual score adjustment";
        }

        ScoreRecordEntity entity = new ScoreRecordEntity();
        entity.setClubId(club.getId());
        entity.setUserId(userId);
        entity.setRuleId(rule == null ? null : rule.getId());
        entity.setScoreDelta(scoreDelta);
        entity.setReason(reason);
        entity.setOperatorUserId(admin.userId());
        ScoreRecordEntity saved = scoreRecordRepository.save(entity);

        auditService.create("ADMIN_SCORE_CREATED", admin.userId(), "SCORE_RECORD", String.valueOf(saved.getId()), "Administrator created a score record");

        Map<Long, UserEntity> studentMap = loadUserMap(List.of(userId));
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(List.of(userId));

        return new AdminScoreRecordResponse(
                saved.getId(),
                club.getId(),
                club.getName(),
                saved.getUserId(),
                defaultString(studentMap.get(userId) != null ? studentMap.get(userId).getUsername() : ""),
                resolveStudentDisplayName(userId, studentMap, studentInfoMap),
                defaultString(studentInfoMap.get(userId) != null ? studentInfoMap.get(userId).getStudentNo() : ""),
                saved.getRuleId(),
                rule == null ? "" : rule.getName(),
                saved.getScoreDelta(),
                saved.getReason(),
                saved.getOperatorUserId(),
                admin.username(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void deleteAdminScore(Long recordId) {
        AuthenticatedUser admin = requireAdmin();
        ScoreRecordEntity record = getRequiredRecord(recordId);
        ensureScoreRecordRevocable(record);
        scoreRecordRepository.delete(record);
        auditService.create("ADMIN_SCORE_DELETED", admin.userId(), "SCORE_RECORD", String.valueOf(recordId), "Administrator revoked a score record");
    }

    @Transactional
    public void deleteClubScoreRecord(Long clubId, Long recordId) {
        AuthenticatedUser operator = requireAdminOrManagerForClub(clubId);
        getRequiredClub(clubId);
        ScoreRecordEntity record = getRequiredRecord(recordId);
        if (!Objects.equals(record.getClubId(), clubId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "score record not found in club");
        }
        if (!operator.role().isSystemAdmin() && !Objects.equals(record.getOperatorUserId(), operator.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager can only revoke own score records");
        }
        ensureScoreRecordRevocable(record);
        scoreRecordRepository.delete(record);
        auditService.create("CLUB_SCORE_DELETED", operator.userId(), "SCORE_RECORD", String.valueOf(recordId), "Club score record revoked");
    }

    @Transactional(readOnly = true)
    public ScoreSummaryResponse getMySummary() {
        AuthenticatedUser student = requireStudent();
        List<ClubStudentMemberEntity> memberships = clubStudentMemberRepository.findAllByStudentUserId(student.userId());
        List<Long> clubIds = memberships.stream().map(ClubStudentMemberEntity::getClubId).distinct().toList();
        Map<Long, ClubEntity> clubMap = loadClubMap(clubIds);
        Map<Long, Long> clubScores = clubIds.isEmpty()
                ? Map.of()
                : scoreRecordRepository.sumByUserIdGroupedByClubIds(student.userId(), clubIds)
                .stream()
                .collect(Collectors.toMap(ClubScoreTotalProjection::getClubId, item -> normalize(item.getTotalScore())));

        List<ScoreSummaryClubResponse> clubs = clubMap.values().stream()
                .sorted(Comparator.comparing(ClubEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(club -> new ScoreSummaryClubResponse(club.getId(), club.getName(), clubScores.getOrDefault(club.getId(), 0L)))
                .toList();

        return new ScoreSummaryResponse(
                scoreBalanceService.getTotalScore(student.userId()),
                scoreBalanceService.getRedeemedScore(student.userId()),
                scoreBalanceService.getBalance(student.userId()),
                clubs
        );
    }

    @Transactional(readOnly = true)
    public List<StudentScoreRecordResponse> getMyScoreRecords() {
        AuthenticatedUser student = requireStudent();
        List<ScoreRecordEntity> records = scoreRecordRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(student.userId());
        return buildStudentScoreRecords(records);
    }

    @Transactional(readOnly = true)
    public List<ClubScoreRankingResponse> getClubRankings(Long clubId, String range) {
        requireStudent();
        getRequiredClub(clubId);
        return buildClubRankings(clubId, range);
    }

    @Transactional(readOnly = true)
    public List<ClubScoreRankingResponse> getClubRankingsForManager(Long clubId, String range) {
        requireAdminOrManagerForClub(clubId);
        getRequiredClub(clubId);
        return buildClubRankings(clubId, range);
    }

    private List<ClubScoreRankingResponse> buildClubRankings(Long clubId, String range) {
        List<Long> studentIds = clubStudentMemberRepository.findAllByClubId(clubId)
                .stream()
                .map(ClubStudentMemberEntity::getStudentUserId)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }

        Instant startAt = resolveRangeStart(range);
        Map<Long, Long> scoreMap = (startAt == null
                ? scoreRecordRepository.sumByClubIdAndUserIds(clubId, studentIds)
                : scoreRecordRepository.sumByClubIdAndUserIdsFrom(clubId, studentIds, startAt))
                .stream()
                .collect(Collectors.toMap(UserScoreTotalProjection::getUserId, item -> normalize(item.getTotalScore())));

        Map<Long, UserEntity> userMap = userRepository.findAllByIdInAndRole(studentIds, UserRole.STUDENT)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, item -> item));
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(studentIds);

        List<ClubScoreRankingResponse> rows = new ArrayList<>();
        for (Long studentId : studentIds) {
            UserEntity user = userMap.get(studentId);
            if (user == null) {
                continue;
            }
            StudentInfoEntity info = studentInfoMap.get(studentId);
            rows.add(new ClubScoreRankingResponse(
                    0,
                    studentId,
                    user.getUsername(),
                    info != null && trimToNull(info.getDisplayName()) != null ? info.getDisplayName() : "",
                    info == null ? "" : defaultString(info.getStudentNo()),
                    scoreMap.getOrDefault(studentId, 0L)
            ));
        }

        rows.sort(Comparator.comparingLong(ClubScoreRankingResponse::score).reversed()
                .thenComparing(ClubScoreRankingResponse::userId));

        List<ClubScoreRankingResponse> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index += 1) {
            ClubScoreRankingResponse row = rows.get(index);
            result.add(new ClubScoreRankingResponse(index + 1, row.userId(), row.username(), row.displayName(), row.studentNo(), row.score()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SchoolScoreRankingResponse> getSchoolRankings() {
        requireStudent();
        List<ClubEntity> clubs = clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE);
        List<Long> clubIds = clubs.stream().map(ClubEntity::getId).toList();
        Map<Long, Long> memberCounts = clubIds.isEmpty()
                ? Map.of()
                : clubStudentMemberRepository.countByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
        Map<Long, Long> totalScores = clubIds.isEmpty()
                ? Map.of()
                : scoreRecordRepository.sumByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubScoreTotalProjection::getClubId, item -> normalize(item.getTotalScore())));

        List<SchoolScoreRankingResponse> rows = clubs.stream()
                .map(club -> {
                    long memberCount = memberCounts.getOrDefault(club.getId(), 0L);
                    long totalScore = totalScores.getOrDefault(club.getId(), 0L);
                    double avgScore = memberCount <= 0 ? 0D : round2((double) totalScore / (double) memberCount);
                    return new SchoolScoreRankingResponse(0, club.getId(), club.getName(), memberCount, totalScore, avgScore);
                })
                .sorted(Comparator.comparingDouble(SchoolScoreRankingResponse::avgScore).reversed()
                        .thenComparing(SchoolScoreRankingResponse::clubId))
                .toList();

        List<SchoolScoreRankingResponse> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index += 1) {
            SchoolScoreRankingResponse row = rows.get(index);
            result.add(new SchoolScoreRankingResponse(index + 1, row.clubId(), row.clubName(), row.memberCount(), row.totalScore(), row.avgScore()));
        }
        return result;
    }

    private void applyRuleMutation(ScoreRuleEntity entity, AdminScoreRuleMutationRequest request) {
        entity.setName(normalizeRuleName(request.name()));
        entity.setScoreDelta(request.scoreDelta());
        entity.setScopeType(parseRuleScope(request.scopeType()));
        entity.setStatus(parseRuleStatus(request.status()));

        if (entity.getScopeType() == ScoreRuleScope.CLUB) {
            if (request.clubId() == null || request.clubId() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "clubId is required for club scope rule");
            }
            ClubEntity club = getRequiredClub(request.clubId());
            if (club.getStatus() != ClubStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.CONFLICT, "club scope rule requires an active club");
            }
            entity.setClubId(request.clubId());
        } else {
            entity.setClubId(null);
        }
    }

    private List<ScoreRuleResponse> buildRuleResponses(List<ScoreRuleEntity> rules) {
        Map<Long, ClubEntity> clubMap = loadClubMap(rules.stream().map(ScoreRuleEntity::getClubId).filter(Objects::nonNull).toList());
        return rules.stream()
                .map(rule -> {
                    ClubEntity club = rule.getClubId() == null ? null : clubMap.get(rule.getClubId());
                    return new ScoreRuleResponse(
                            rule.getId(),
                            rule.getName(),
                            rule.getScoreDelta(),
                            rule.getScopeType().name(),
                            rule.getClubId(),
                            club == null ? "" : club.getName(),
                            rule.getStatus().name(),
                            rule.getCreatedBy(),
                            rule.getCreatedAt(),
                            rule.getUpdatedAt()
                    );
                })
                .toList();
    }

    private ScoreRuleEntity getRequiredRule(Long ruleId) {
        return scoreRuleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "score rule not found"));
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private ScoreRecordEntity getRequiredRecord(Long recordId) {
        return scoreRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "score record not found"));
    }

    private void ensureScoreRecordRevocable(ScoreRecordEntity record) {
        long balanceAfter = scoreBalanceService.getBalance(record.getUserId()) - record.getScoreDelta();
        if (balanceAfter < 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "revoking this record would make the student score balance negative");
        }
    }

    private List<AdminScoreRecordResponse> buildAdminScoreRecords(List<ScoreRecordEntity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> clubIds = records.stream().map(ScoreRecordEntity::getClubId).filter(Objects::nonNull).distinct().toList();
        List<Long> userIds = records.stream().map(ScoreRecordEntity::getUserId).filter(Objects::nonNull).distinct().toList();
        List<Long> operatorIds = records.stream().map(ScoreRecordEntity::getOperatorUserId).filter(Objects::nonNull).distinct().toList();
        List<Long> ruleIds = records.stream().map(ScoreRecordEntity::getRuleId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ClubEntity> clubMap = loadClubMap(clubIds);
        Map<Long, UserEntity> studentMap = loadUserMap(userIds);
        Map<Long, UserEntity> operatorMap = loadUserMap(operatorIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(userIds);
        Map<Long, ScoreRuleEntity> ruleMap = ruleIds.isEmpty()
                ? Map.of()
                : scoreRuleRepository.findAllById(ruleIds).stream()
                .collect(Collectors.toMap(ScoreRuleEntity::getId, rule -> rule));

        return records.stream()
                .map(record -> {
                    ClubEntity club = record.getClubId() == null ? null : clubMap.get(record.getClubId());
                    UserEntity student = record.getUserId() == null ? null : studentMap.get(record.getUserId());
                    UserEntity operator = record.getOperatorUserId() == null ? null : operatorMap.get(record.getOperatorUserId());
                    ScoreRuleEntity rule = record.getRuleId() == null ? null : ruleMap.get(record.getRuleId());
                    StudentInfoEntity studentInfo = record.getUserId() == null ? null : studentInfoMap.get(record.getUserId());
                    return new AdminScoreRecordResponse(
                            record.getId(),
                            record.getClubId(),
                            club == null ? "" : club.getName(),
                            record.getUserId(),
                            defaultString(student == null ? "" : student.getUsername()),
                            record.getUserId() == null ? "" : resolveStudentDisplayName(record.getUserId(), studentMap, studentInfoMap),
                            defaultString(studentInfo == null ? "" : studentInfo.getStudentNo()),
                            record.getRuleId(),
                            rule == null ? "" : rule.getName(),
                            record.getScoreDelta(),
                            record.getReason(),
                            record.getOperatorUserId(),
                            defaultString(operator == null ? "" : operator.getUsername()),
                            record.getCreatedAt()
                    );
                })
                .toList();
    }

    private List<StudentScoreRecordResponse> buildStudentScoreRecords(List<ScoreRecordEntity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> clubIds = records.stream().map(ScoreRecordEntity::getClubId).filter(Objects::nonNull).distinct().toList();
        List<Long> operatorIds = records.stream().map(ScoreRecordEntity::getOperatorUserId).filter(Objects::nonNull).distinct().toList();
        List<Long> ruleIds = records.stream().map(ScoreRecordEntity::getRuleId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ClubEntity> clubMap = loadClubMap(clubIds);
        Map<Long, UserEntity> operatorMap = loadUserMap(operatorIds);
        Map<Long, ScoreRuleEntity> ruleMap = ruleIds.isEmpty()
                ? Map.of()
                : scoreRuleRepository.findAllById(ruleIds).stream()
                .collect(Collectors.toMap(ScoreRuleEntity::getId, rule -> rule));

        return records.stream()
                .map(record -> {
                    ClubEntity club = record.getClubId() == null ? null : clubMap.get(record.getClubId());
                    UserEntity operator = record.getOperatorUserId() == null ? null : operatorMap.get(record.getOperatorUserId());
                    ScoreRuleEntity rule = record.getRuleId() == null ? null : ruleMap.get(record.getRuleId());
                    return new StudentScoreRecordResponse(
                            record.getId(),
                            record.getClubId(),
                            club == null ? "" : club.getName(),
                            record.getRuleId(),
                            rule == null ? "" : rule.getName(),
                            record.getScoreDelta(),
                            record.getReason(),
                            defaultString(operator == null ? "" : operator.getUsername()),
                            record.getCreatedAt()
                    );
                })
                .toList();
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return currentUser;
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return currentUser;
    }

    private AuthenticatedUser requireAdminOrManagerForClub(Long clubId) {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role().isSystemAdmin()) {
            return currentUser;
        }
        if (currentUser.role() == UserRole.CLUB_MANAGER
                && clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, currentUser.userId())) {
            return currentUser;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "club score permission denied");
    }

    private ScoreRuleScope parseRuleScope(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score rule scope is required");
        }
        try {
            return ScoreRuleScope.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score rule scope is invalid");
        }
    }

    private ScoreRuleStatus parseRuleStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score rule status is required");
        }
        try {
            return ScoreRuleStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score rule status is invalid");
        }
    }

    private String normalizeRuleName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "rule name is required");
        }
        return normalized;
    }

    private String resolveStudentDisplayName(Long userId,
                                             Map<Long, UserEntity> userMap,
                                             Map<Long, StudentInfoEntity> studentInfoMap) {
        StudentInfoEntity info = studentInfoMap.get(userId);
        if (info != null && trimToNull(info.getDisplayName()) != null) {
            return info.getDisplayName();
        }
        return "";
    }

    private Instant resolveRangeStart(String range) {
        String normalized = String.valueOf(range == null ? "all" : range).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "all" -> null;
            case "week" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "term" -> Instant.now().minus(120, ChronoUnit.DAYS);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ranking range is invalid");
        };
    }

    private Map<Long, ClubEntity> loadClubMap(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubRepository.findAllById(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubEntity::getId, item -> item));
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> map = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return studentInfoRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(StudentInfoEntity::getUserId, item -> item));
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
