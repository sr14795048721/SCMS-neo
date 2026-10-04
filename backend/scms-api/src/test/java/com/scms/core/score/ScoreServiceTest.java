package com.scms.core.score;

import com.scms.core.audit.service.AuditService;
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
import com.scms.core.score.dto.AdminScoreRecordMutationRequest;
import com.scms.core.score.dto.AdminScoreRecordResponse;
import com.scms.core.score.dto.AdminScoreRuleMutationRequest;
import com.scms.core.score.dto.ScoreRuleResponse;
import com.scms.core.score.repository.ScoreRecordRepository;
import com.scms.core.score.repository.ScoreRuleRepository;
import com.scms.core.score.service.ScoreBalanceService;
import com.scms.core.score.service.ScoreService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreServiceTest {

    private ScoreRuleRepository scoreRuleRepository;
    private ScoreRecordRepository scoreRecordRepository;
    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private ScoreBalanceService scoreBalanceService;
    private ScoreService scoreService;

    @BeforeEach
    void setUp() {
        scoreRuleRepository = mock(ScoreRuleRepository.class);
        scoreRecordRepository = mock(ScoreRecordRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        scoreBalanceService = mock(ScoreBalanceService.class);

        scoreService = new ScoreService(
                scoreRuleRepository,
                scoreRecordRepository,
                clubRepository,
                clubStudentMemberRepository,
                clubManagerBindingRepository,
                userRepository,
                studentInfoRepository,
                currentUserProvider,
                auditService,
                scoreBalanceService
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void deleteRuleShouldRejectReferencedRule() {
        ScoreRuleEntity rule = new ScoreRuleEntity();
        setRuleId(rule, 9L);

        when(scoreRuleRepository.findById(9L)).thenReturn(Optional.of(rule));
        when(scoreRecordRepository.countByRuleId(9L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> scoreService.deleteRule(9L));

        assertEquals(ErrorCode.DEPENDENCY_EXISTS, exception.getErrorCode());
        verifyNoInteractions(auditService);
    }

    @Test
    void createGlobalRuleShouldSucceed() {
        AdminScoreRuleMutationRequest request = new AdminScoreRuleMutationRequest(
                "Global rule",
                5,
                "GLOBAL",
                null,
                "ACTIVE"
        );

        when(scoreRuleRepository.save(any(ScoreRuleEntity.class))).thenAnswer(invocation -> {
            ScoreRuleEntity entity = invocation.getArgument(0);
            setRuleId(entity, 12L);
            setRuleTimestamps(entity);
            entity.setScopeType(ScoreRuleScope.GLOBAL);
            entity.setStatus(ScoreRuleStatus.ACTIVE);
            return entity;
        });

        ScoreRuleResponse response = scoreService.createRule(request);

        assertEquals(12L, response.id());
        assertEquals("Global rule", response.name());
        assertEquals(5, response.scoreDelta());
        assertEquals("GLOBAL", response.scopeType());
        assertNull(response.clubId());
        assertEquals("", response.clubName());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void managerClubRankingShouldSucceed() {
        ClubStudentMemberEntity member = new ClubStudentMemberEntity();
        member.setClubId(2L);
        member.setStudentUserId(11L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(3L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(2L, 3L)).thenReturn(true);
        when(clubRepository.findById(2L)).thenReturn(Optional.of(new com.scms.core.club.domain.ClubEntity()));
        when(clubStudentMemberRepository.findAllByClubId(2L)).thenReturn(java.util.List.of(member));
        when(scoreRecordRepository.sumByClubIdAndUserIds(2L, java.util.List.of(11L))).thenReturn(java.util.List.of());
        when(userRepository.findAllByIdInAndRole(java.util.List.of(11L), UserRole.STUDENT)).thenReturn(java.util.List.of());
        when(studentInfoRepository.findAllByUserIdIn(java.util.List.of(11L))).thenReturn(java.util.List.of());

        assertTrue(scoreService.getClubRankingsForManager(2L, "all").isEmpty());
    }

    @Test
    void adminAddScoreWithRuleShouldUseRuleDelta() {
        com.scms.core.club.domain.ClubEntity club = new com.scms.core.club.domain.ClubEntity();
        setEntityId(club, 2L);
        club.setName("Club A");

        ScoreRuleEntity rule = new ScoreRuleEntity();
        setRuleId(rule, 9L);
        rule.setName("Bonus");
        rule.setScoreDelta(5);
        rule.setScopeType(ScoreRuleScope.GLOBAL);
        rule.setStatus(ScoreRuleStatus.ACTIVE);

        when(clubRepository.findById(2L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(2L, 11L)).thenReturn(true);
        when(scoreRuleRepository.findById(9L)).thenReturn(Optional.of(rule));
        when(scoreRecordRepository.save(any(ScoreRecordEntity.class))).thenAnswer(invocation -> {
            ScoreRecordEntity entity = invocation.getArgument(0);
            setEntityId(entity, 100L);
            return entity;
        });

        AdminScoreRecordResponse response = scoreService.addAdminScore(
                new AdminScoreRecordMutationRequest(2L, 11L, 9L, 3, "Good performance"));

        assertEquals(100L, response.id());
        assertEquals(2L, response.clubId());
        assertEquals("Club A", response.clubName());
        assertEquals(11L, response.userId());
        assertEquals(9L, response.ruleId());
        assertEquals("Bonus", response.ruleName());
        assertEquals(5, response.scoreDelta());
        assertEquals("Good performance", response.reason());
        assertEquals(1L, response.operatorUserId());
        assertEquals("admin", response.operatorName());
    }

    @Test
    void adminAddScoreWithoutRuleShouldKeepCustomDelta() {
        com.scms.core.club.domain.ClubEntity club = new com.scms.core.club.domain.ClubEntity();
        setEntityId(club, 2L);
        club.setName("Club A");

        when(clubRepository.findById(2L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(2L, 11L)).thenReturn(true);
        when(scoreRecordRepository.save(any(ScoreRecordEntity.class))).thenAnswer(invocation -> {
            ScoreRecordEntity entity = invocation.getArgument(0);
            setEntityId(entity, 101L);
            return entity;
        });

        AdminScoreRecordResponse response = scoreService.addAdminScore(
                new AdminScoreRecordMutationRequest(2L, 11L, null, -3, null));

        assertEquals(101L, response.id());
        assertNull(response.ruleId());
        assertEquals("", response.ruleName());
        assertEquals(-3, response.scoreDelta());
        assertEquals("Manual score adjustment", response.reason());
    }

    @Test
    void adminAddScoreShouldRejectNonMember() {
        when(clubRepository.findById(2L)).thenReturn(Optional.of(new com.scms.core.club.domain.ClubEntity()));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(2L, 11L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scoreService.addAdminScore(new AdminScoreRecordMutationRequest(2L, 11L, null, 3, "x")));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(scoreRecordRepository, never()).save(any(ScoreRecordEntity.class));
    }

    @Test
    void adminShouldRevokeAnyScoreRecord() {
        ScoreRecordEntity record = buildRecord(2L, 11L, 99L, 5);
        setEntityId(record, 100L);
        when(scoreRecordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(scoreBalanceService.getBalance(11L)).thenReturn(5L);

        scoreService.deleteAdminScore(100L);

        verify(scoreRecordRepository).delete(record);
    }

    @Test
    void revokeShouldRejectNegativeBalance() {
        ScoreRecordEntity record = buildRecord(2L, 11L, 99L, 5);
        setEntityId(record, 100L);
        when(scoreRecordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(scoreBalanceService.getBalance(11L)).thenReturn(4L);

        BusinessException exception = assertThrows(BusinessException.class, () -> scoreService.deleteAdminScore(100L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(scoreRecordRepository, never()).delete(any(ScoreRecordEntity.class));
    }

    @Test
    void managerShouldRevokeOwnScoreRecord() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(3L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(2L, 3L)).thenReturn(true);
        when(clubRepository.findById(2L)).thenReturn(Optional.of(new com.scms.core.club.domain.ClubEntity()));

        ScoreRecordEntity record = buildRecord(2L, 11L, 3L, 5);
        setEntityId(record, 100L);
        when(scoreRecordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(scoreBalanceService.getBalance(11L)).thenReturn(5L);

        scoreService.deleteClubScoreRecord(2L, 100L);

        verify(scoreRecordRepository).delete(record);
    }

    @Test
    void managerShouldRejectRevokingOthersRecord() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(3L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(2L, 3L)).thenReturn(true);
        when(clubRepository.findById(2L)).thenReturn(Optional.of(new com.scms.core.club.domain.ClubEntity()));

        ScoreRecordEntity record = buildRecord(2L, 11L, 99L, 5);
        setEntityId(record, 100L);
        when(scoreRecordRepository.findById(100L)).thenReturn(Optional.of(record));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scoreService.deleteClubScoreRecord(2L, 100L));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(scoreRecordRepository, never()).delete(any(ScoreRecordEntity.class));
    }

    private ScoreRecordEntity buildRecord(Long clubId, Long userId, Long operatorUserId, int scoreDelta) {
        ScoreRecordEntity record = new ScoreRecordEntity();
        record.setClubId(clubId);
        record.setUserId(userId);
        record.setScoreDelta(scoreDelta);
        record.setReason("recorded");
        record.setOperatorUserId(operatorUserId);
        return record;
    }

    private void setEntityId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setRuleId(ScoreRuleEntity entity, Long id) {
        try {
            var field = ScoreRuleEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setRuleTimestamps(ScoreRuleEntity entity) {
        try {
            var createdAt = ScoreRuleEntity.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(entity, java.time.Instant.now());
            var updatedAt = ScoreRuleEntity.class.getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(entity, java.time.Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
