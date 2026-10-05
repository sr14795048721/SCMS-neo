package com.scms.core.attendance;

import com.scms.core.attendance.domain.AttendanceRecordEntity;
import com.scms.core.attendance.domain.AttendanceRecordStatus;
import com.scms.core.attendance.domain.AttendanceSessionEntity;
import com.scms.core.attendance.domain.AttendanceSessionStatus;
import com.scms.core.attendance.dto.PublicAttendanceSessionResponse;
import com.scms.core.attendance.dto.PublicAttendanceSignRequest;
import com.scms.core.attendance.dto.PublicAttendanceSignResponse;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.attendance.repository.AttendanceSessionRepository;
import com.scms.core.attendance.service.AttendanceSignatureStorageService;
import com.scms.core.attendance.service.PublicAttendanceService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubMemberRole;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.score.domain.ScoreRuleEntity;
import com.scms.core.score.domain.ScoreRuleScope;
import com.scms.core.score.domain.ScoreRuleStatus;
import com.scms.core.score.repository.ScoreRuleRepository;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicAttendanceServiceTest {

    private static final String SHARE_TOKEN = "token-abc";
    private static final String SIGNATURE_DATA_URL = "data:image/png;base64,iVBORw0KGgo=";

    private AttendanceSessionRepository attendanceSessionRepository;
    private AttendanceRecordRepository attendanceRecordRepository;
    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ScoreRuleRepository scoreRuleRepository;
    private StudentInfoRepository studentInfoRepository;
    private AttendanceSignatureStorageService signatureStorageService;
    private PublicAttendanceService service;

    @BeforeEach
    void setUp() {
        attendanceSessionRepository = mock(AttendanceSessionRepository.class);
        attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        scoreRuleRepository = mock(ScoreRuleRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        signatureStorageService = mock(AttendanceSignatureStorageService.class);
        service = new PublicAttendanceService(
                attendanceSessionRepository,
                attendanceRecordRepository,
                clubRepository,
                clubStudentMemberRepository,
                scoreRuleRepository,
                studentInfoRepository,
                signatureStorageService
        );
    }

    @Test
    void getSessionShouldReturnOverviewByShareToken() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.OPEN);
        ClubEntity club = club(3L, "Art Club");
        ScoreRuleEntity rule = scoreRule(7L, "签到积分", 2);

        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));
        when(clubRepository.findById(3L)).thenReturn(Optional.of(club));
        when(scoreRuleRepository.findById(7L)).thenReturn(Optional.of(rule));

        PublicAttendanceSessionResponse response = service.getSession(SHARE_TOKEN);

        assertEquals("Art Club", response.clubName());
        assertEquals("第1次社团课", response.title());
        assertEquals(AttendanceSessionStatus.OPEN.name(), response.status());
        assertEquals("签到积分", response.scoreRuleName());
        assertEquals(2, response.scoreDelta());
        assertNotNull(response.startedAt());
    }

    @Test
    void getSessionShouldRejectUnknownToken() {
        when(attendanceSessionRepository.findByShareToken("missing")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getSession("missing")
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void signShouldMatchMemberByNameAndStudentNoAndArchiveSignature() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.OPEN);
        ClubStudentMemberEntity member = member(3L, 21L, ClubMemberRole.MINISTER);
        StudentInfoEntity info = studentInfo(21L, "张三", "2025001");
        AttendanceRecordEntity record = attendanceRecord(15L, 21L, AttendanceRecordStatus.PENDING);

        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(info));
        when(attendanceRecordRepository.findBySessionIdAndStudentUserId(15L, 21L)).thenReturn(Optional.of(record));
        when(signatureStorageService.storeSignature(SIGNATURE_DATA_URL)).thenReturn("sig-1.png");

        PublicAttendanceSignResponse response = service.sign(
                SHARE_TOKEN,
                new PublicAttendanceSignRequest(" 张三 ", "2025001", SIGNATURE_DATA_URL)
        );

        assertEquals("张三", response.displayName());
        assertEquals("MINISTER", response.role());
        assertNotNull(response.checkInAt());
        assertEquals(AttendanceRecordStatus.CHECKED_IN, record.getStatus());
        assertEquals("张三", record.getSignedName());
        assertEquals("MINISTER", record.getSignedRole());
        assertEquals("sig-1.png", record.getSignaturePath());
        assertNotNull(record.getCheckInAt());
    }

    @Test
    void signShouldRejectWhenIdentityDoesNotMatch() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.OPEN);
        ClubStudentMemberEntity member = member(3L, 21L, ClubMemberRole.MEMBER);
        StudentInfoEntity info = studentInfo(21L, "张三", "2025001");

        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(info));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sign(SHARE_TOKEN, new PublicAttendanceSignRequest("张三", "wrong-no", SIGNATURE_DATA_URL))
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(signatureStorageService, never()).storeSignature(any());
    }

    @Test
    void signShouldRejectDuplicateSign() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.OPEN);
        ClubStudentMemberEntity member = member(3L, 21L, ClubMemberRole.MEMBER);
        StudentInfoEntity info = studentInfo(21L, "张三", "2025001");
        AttendanceRecordEntity record = attendanceRecord(15L, 21L, AttendanceRecordStatus.CHECKED_IN);
        record.setCheckInAt(java.time.Instant.parse("2026-04-06T09:00:00Z"));

        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(info));
        when(attendanceRecordRepository.findBySessionIdAndStudentUserId(15L, 21L)).thenReturn(Optional.of(record));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sign(SHARE_TOKEN, new PublicAttendanceSignRequest("张三", "2025001", SIGNATURE_DATA_URL))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(signatureStorageService, never()).storeSignature(any());
    }

    @Test
    void signShouldRejectWhenSessionCompleted() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.COMPLETED);
        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sign(SHARE_TOKEN, new PublicAttendanceSignRequest("张三", "2025001", SIGNATURE_DATA_URL))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(clubStudentMemberRepository, never()).findAllByClubId(any());
    }

    @Test
    void signShouldAutoCreateRecordWhenMemberJoinedAfterSessionCreated() {
        AttendanceSessionEntity session = session(AttendanceSessionStatus.OPEN);
        ClubStudentMemberEntity member = member(3L, 21L, ClubMemberRole.MEMBER);
        StudentInfoEntity info = studentInfo(21L, "张三", "2025001");

        when(attendanceSessionRepository.findByShareToken(SHARE_TOKEN)).thenReturn(Optional.of(session));
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(info));
        when(attendanceRecordRepository.findBySessionIdAndStudentUserId(15L, 21L)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(signatureStorageService.storeSignature(SIGNATURE_DATA_URL)).thenReturn("sig-1.png");

        PublicAttendanceSignResponse response = service.sign(
                SHARE_TOKEN,
                new PublicAttendanceSignRequest("张三", "2025001", SIGNATURE_DATA_URL)
        );

        assertEquals("张三", response.displayName());
        assertNotNull(response.checkInAt());
        verify(attendanceRecordRepository, times(2)).save(any());
    }

    private AttendanceSessionEntity session(AttendanceSessionStatus status) {
        AttendanceSessionEntity entity = new AttendanceSessionEntity();
        setId(entity, 15L);
        entity.setClubId(3L);
        entity.setTitle("第1次社团课");
        entity.setScoreRuleId(7L);
        entity.setCreatedBy(9L);
        entity.setStatus(status);
        entity.setShareToken(SHARE_TOKEN);
        entity.setStartedAt(java.time.Instant.parse("2026-04-06T08:30:00Z"));
        return entity;
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity entity = new ClubEntity();
        setId(entity, id);
        entity.setName(name);
        return entity;
    }

    private ClubStudentMemberEntity member(Long clubId, Long userId, ClubMemberRole role) {
        ClubStudentMemberEntity entity = new ClubStudentMemberEntity();
        entity.setClubId(clubId);
        entity.setStudentUserId(userId);
        entity.setRole(role);
        return entity;
    }

    private ScoreRuleEntity scoreRule(Long id, String name, int scoreDelta) {
        ScoreRuleEntity entity = new ScoreRuleEntity();
        setId(entity, id);
        entity.setName(name);
        entity.setScoreDelta(scoreDelta);
        entity.setScopeType(ScoreRuleScope.CLUB);
        entity.setStatus(ScoreRuleStatus.ACTIVE);
        return entity;
    }

    private StudentInfoEntity studentInfo(Long userId, String displayName, String studentNo) {
        StudentInfoEntity entity = new StudentInfoEntity();
        setId(entity, userId);
        entity.setUserId(userId);
        entity.setDisplayName(displayName);
        entity.setStudentNo(studentNo);
        return entity;
    }

    private AttendanceRecordEntity attendanceRecord(Long sessionId, Long studentUserId, AttendanceRecordStatus status) {
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setSessionId(sessionId);
        entity.setStudentUserId(studentUserId);
        entity.setStatus(status);
        return entity;
    }

    private void setId(Object target, Long id) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException("id field not found");
    }
}
