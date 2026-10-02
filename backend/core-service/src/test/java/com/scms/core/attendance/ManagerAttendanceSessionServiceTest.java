package com.scms.core.attendance;

import com.scms.core.attendance.domain.AttendanceRecordEntity;
import com.scms.core.attendance.domain.AttendanceRecordStatus;
import com.scms.core.attendance.domain.AttendanceSessionEntity;
import com.scms.core.attendance.domain.AttendanceSessionStatus;
import com.scms.core.attendance.dto.ManagerAttendanceMarkRequest;
import com.scms.core.attendance.dto.ManagerAttendanceBulkMarkRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionCreateRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionDetailResponse;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.attendance.repository.AttendanceSessionRepository;
import com.scms.core.attendance.service.ManagerAttendanceSessionService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerAttendanceSessionServiceTest {

    private AttendanceSessionRepository attendanceSessionRepository;
    private AttendanceRecordRepository attendanceRecordRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ScoreRuleRepository scoreRuleRepository;
    private ScoreRecordRepository scoreRecordRepository;
    private StudentInfoRepository studentInfoRepository;
    private UserRepository userRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private ManagerAttendanceSessionService service;

    @BeforeEach
    void setUp() {
        attendanceSessionRepository = mock(AttendanceSessionRepository.class);
        attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        scoreRuleRepository = mock(ScoreRuleRepository.class);
        scoreRecordRepository = mock(ScoreRecordRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        userRepository = mock(UserRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        service = new ManagerAttendanceSessionService(
                attendanceSessionRepository,
                attendanceRecordRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                scoreRuleRepository,
                scoreRecordRepository,
                studentInfoRepository,
                userRepository,
                currentUserProvider,
                auditService
        );
    }

    @Test
    void createSessionShouldSnapshotCurrentMembers() {
        ClubEntity club = club(3L, "Art Club");
        ScoreRuleEntity rule = scoreRule(7L, "签到积分", 2, 3L);
        ClubStudentMemberEntity firstMember = member(3L, 21L);
        ClubStudentMemberEntity secondMember = member(3L, 22L);
        UserEntity firstStudent = studentUser(21L, "student-21");
        UserEntity secondStudent = studentUser(22L, "student-22");
        StudentInfoEntity firstInfo = studentInfo(21L, "张三", "HIGH_1", "1");
        StudentInfoEntity secondInfo = studentInfo(22L, "李四", "HIGH_2", "2");
        List<AttendanceRecordEntity> savedRecords = new ArrayList<>();
        Map<Long, AttendanceSessionEntity> sessions = new LinkedHashMap<>();

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 9L)).thenReturn(true);
        when(clubRepository.findById(3L)).thenReturn(Optional.of(club));
        when(scoreRuleRepository.findById(7L)).thenReturn(Optional.of(rule));
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(firstMember, secondMember));
        when(scoreRuleRepository.findAllById(List.of(7L))).thenReturn(List.of(rule));
        when(userRepository.findAllByIdInAndRole(anyCollection(), org.mockito.ArgumentMatchers.eq(UserRole.STUDENT)))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(firstInfo, secondInfo));

        when(attendanceSessionRepository.save(any(AttendanceSessionEntity.class))).thenAnswer(invocation -> {
            AttendanceSessionEntity entity = invocation.getArgument(0);
            setId(entity, 15L);
            sessions.put(15L, entity);
            return entity;
        });
        doAnswer(invocation -> {
            List<AttendanceRecordEntity> records = invocation.getArgument(0);
            savedRecords.clear();
            savedRecords.addAll(records);
            long nextId = 1L;
            for (AttendanceRecordEntity record : records) {
                setId(record, nextId++);
            }
            return records;
        }).when(attendanceRecordRepository).saveAll(anyCollection());
        when(attendanceRecordRepository.findAllBySessionIdIn(List.of(15L))).thenAnswer(invocation -> List.copyOf(savedRecords));
        when(attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(15L)).thenAnswer(invocation -> List.copyOf(savedRecords));

        ManagerAttendanceSessionDetailResponse response = service.createSession(3L, new ManagerAttendanceSessionCreateRequest("第1次社团课", 7L));

        assertEquals(15L, response.session().sessionId());
        assertEquals(2, response.members().size());
        assertEquals("张三", response.members().getFirst().displayName());
        assertEquals(AttendanceRecordStatus.PENDING.name(), response.members().getFirst().status());
        verify(attendanceRecordRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    void settleShouldOnlyCreateScoresForCheckedOutMembers() {
        AttendanceSessionEntity session = new AttendanceSessionEntity();
        setId(session, 18L);
        session.setClubId(3L);
        session.setTitle("第2次社团课");
        session.setScoreRuleId(7L);
        session.setCreatedBy(9L);
        session.setStatus(AttendanceSessionStatus.OPEN);

        ScoreRuleEntity rule = scoreRule(7L, "签到积分", 3, 3L);
        AttendanceRecordEntity checkedOut = attendanceRecord(18L, 21L, AttendanceRecordStatus.CHECKED_OUT);
        checkedOut.setCheckInAt(Instant.parse("2026-04-06T09:00:00Z"));
        checkedOut.setCheckOutAt(Instant.parse("2026-04-06T10:00:00Z"));
        AttendanceRecordEntity pending = attendanceRecord(18L, 22L, AttendanceRecordStatus.PENDING);
        List<AttendanceRecordEntity> records = new ArrayList<>(List.of(checkedOut, pending));
        List<ScoreRecordEntity> savedScoreRecords = new ArrayList<>();

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 9L)).thenReturn(true);
        when(attendanceSessionRepository.findByIdAndClubId(18L, 3L)).thenReturn(Optional.of(session));
        when(scoreRuleRepository.findById(7L)).thenReturn(Optional.of(rule));
        when(scoreRuleRepository.findAllById(List.of(7L))).thenReturn(List.of(rule));
        when(attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(18L)).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.findAllBySessionIdIn(List.of(18L))).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceSessionRepository.save(any(AttendanceSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByIdInAndRole(anyCollection(), org.mockito.ArgumentMatchers.eq(UserRole.STUDENT))).thenReturn(List.of(
                studentUser(21L, "student-21"),
                studentUser(22L, "student-22")
        ));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(
                studentInfo(21L, "张三", "HIGH_1", "1"),
                studentInfo(22L, "李四", "HIGH_2", "2")
        ));
        when(scoreRecordRepository.save(any(ScoreRecordEntity.class))).thenAnswer(invocation -> {
            ScoreRecordEntity entity = invocation.getArgument(0);
            savedScoreRecords.add(entity);
            return entity;
        });

        ManagerAttendanceSessionDetailResponse response = service.settle(3L, 18L);

        assertEquals(AttendanceSessionStatus.COMPLETED.name(), response.session().status());
        assertEquals(1, response.session().settledCount());
        assertEquals(1, savedScoreRecords.size());
        assertEquals(18L, savedScoreRecords.getFirst().getAttendanceSessionId());
        assertEquals(3, savedScoreRecords.getFirst().getScoreDelta());
        assertEquals("社团课签到：第2次社团课", savedScoreRecords.getFirst().getReason());
        verify(scoreRecordRepository).deleteAllByAttendanceSessionId(18L);
    }

    @Test
    void bulkCheckInShouldUpdateSelectedRecordsOnly() {
        AttendanceSessionEntity session = new AttendanceSessionEntity();
        setId(session, 18L);
        session.setClubId(3L);
        session.setTitle("第 3 次社团课");
        session.setScoreRuleId(7L);
        session.setCreatedBy(9L);
        session.setStatus(AttendanceSessionStatus.OPEN);

        AttendanceRecordEntity first = attendanceRecord(18L, 21L, AttendanceRecordStatus.PENDING);
        AttendanceRecordEntity second = attendanceRecord(18L, 22L, AttendanceRecordStatus.PENDING);
        AttendanceRecordEntity third = attendanceRecord(18L, 23L, AttendanceRecordStatus.PENDING);
        List<AttendanceRecordEntity> records = new ArrayList<>(List.of(first, second, third));

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 9L)).thenReturn(true);
        when(attendanceSessionRepository.findByIdAndClubId(18L, 3L)).thenReturn(Optional.of(session));
        when(attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(18L)).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.findAllBySessionIdIn(List.of(18L))).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoreRuleRepository.findAllById(List.of(7L))).thenReturn(List.of(scoreRule(7L, "签到积分", 3, 3L)));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 21L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 22L)).thenReturn(true);
        when(userRepository.findAllByIdInAndRole(anyCollection(), org.mockito.ArgumentMatchers.eq(UserRole.STUDENT))).thenReturn(List.of(
                studentUser(21L, "student-21"),
                studentUser(22L, "student-22"),
                studentUser(23L, "student-23")
        ));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(
                studentInfo(21L, "张三", "HIGH_1", "1"),
                studentInfo(22L, "李四", "HIGH_2", "2"),
                studentInfo(23L, "王五", "HIGH_3", "3")
        ));

        ManagerAttendanceSessionDetailResponse response = service.bulkCheckIn(3L, 18L, new ManagerAttendanceBulkMarkRequest(List.of(21L, 22L)));

        assertEquals(AttendanceRecordStatus.CHECKED_IN.name(), response.members().get(0).status());
        assertEquals(AttendanceRecordStatus.CHECKED_IN.name(), response.members().get(1).status());
        assertEquals(AttendanceRecordStatus.PENDING.name(), response.members().get(2).status());
    }

    @Test
    void bulkCheckOutShouldSkipMembersWhoAreNotCheckedIn() {
        AttendanceSessionEntity session = new AttendanceSessionEntity();
        setId(session, 18L);
        session.setClubId(3L);
        session.setTitle("第 3 次社团课");
        session.setScoreRuleId(7L);
        session.setCreatedBy(9L);
        session.setStatus(AttendanceSessionStatus.OPEN);

        AttendanceRecordEntity first = attendanceRecord(18L, 21L, AttendanceRecordStatus.CHECKED_IN);
        first.setCheckInAt(Instant.parse("2026-04-06T09:00:00Z"));
        AttendanceRecordEntity second = attendanceRecord(18L, 22L, AttendanceRecordStatus.PENDING);
        List<AttendanceRecordEntity> records = new ArrayList<>(List.of(first, second));

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 9L)).thenReturn(true);
        when(attendanceSessionRepository.findByIdAndClubId(18L, 3L)).thenReturn(Optional.of(session));
        when(attendanceRecordRepository.findAllBySessionIdOrderByCreatedAtAscIdAsc(18L)).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.findAllBySessionIdIn(List.of(18L))).thenAnswer(invocation -> List.copyOf(records));
        when(attendanceRecordRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoreRuleRepository.findAllById(List.of(7L))).thenReturn(List.of(scoreRule(7L, "签到积分", 3, 3L)));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 21L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 22L)).thenReturn(true);
        when(userRepository.findAllByIdInAndRole(anyCollection(), org.mockito.ArgumentMatchers.eq(UserRole.STUDENT))).thenReturn(List.of(
                studentUser(21L, "student-21"),
                studentUser(22L, "student-22")
        ));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(
                studentInfo(21L, "张三", "HIGH_1", "1"),
                studentInfo(22L, "李四", "HIGH_2", "2")
        ));

        ManagerAttendanceSessionDetailResponse response = service.bulkCheckOut(3L, 18L, new ManagerAttendanceBulkMarkRequest(List.of(21L, 22L)));

        assertEquals(AttendanceRecordStatus.CHECKED_OUT.name(), response.members().get(0).status());
        assertEquals(AttendanceRecordStatus.PENDING.name(), response.members().get(1).status());
    }

    @Test
    void checkOutShouldRejectWhenStudentHasNotCheckedIn() {
        AttendanceSessionEntity session = new AttendanceSessionEntity();
        setId(session, 18L);
        session.setClubId(3L);
        session.setTitle("第2次社团课");
        session.setScoreRuleId(7L);
        session.setCreatedBy(9L);
        session.setStatus(AttendanceSessionStatus.OPEN);

        AttendanceRecordEntity pending = attendanceRecord(18L, 22L, AttendanceRecordStatus.PENDING);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 9L)).thenReturn(true);
        when(attendanceSessionRepository.findByIdAndClubId(18L, 3L)).thenReturn(Optional.of(session));
        when(attendanceRecordRepository.findBySessionIdAndStudentUserId(18L, 22L)).thenReturn(Optional.of(pending));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 22L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.checkOut(3L, 18L, new ManagerAttendanceMarkRequest(22L))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("student must be checked in before check out", exception.getMessage());
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity entity = new ClubEntity();
        setId(entity, id);
        entity.setName(name);
        return entity;
    }

    private ClubStudentMemberEntity member(Long clubId, Long userId) {
        ClubStudentMemberEntity entity = new ClubStudentMemberEntity();
        entity.setClubId(clubId);
        entity.setStudentUserId(userId);
        return entity;
    }

    private ScoreRuleEntity scoreRule(Long id, String name, int scoreDelta, Long clubId) {
        ScoreRuleEntity entity = new ScoreRuleEntity();
        setId(entity, id);
        entity.setName(name);
        entity.setScoreDelta(scoreDelta);
        entity.setClubId(clubId);
        entity.setScopeType(ScoreRuleScope.CLUB);
        entity.setStatus(ScoreRuleStatus.ACTIVE);
        return entity;
    }

    private UserEntity studentUser(Long id, String username) {
        UserEntity entity = new UserEntity();
        setId(entity, id);
        entity.setUsername(username);
        entity.setRole(UserRole.STUDENT);
        return entity;
    }

    private StudentInfoEntity studentInfo(Long userId, String displayName, String grade, String className) {
        StudentInfoEntity entity = new StudentInfoEntity();
        setId(entity, userId);
        entity.setUserId(userId);
        entity.setDisplayName(displayName);
        entity.setGrade(grade);
        entity.setClassName(className);
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
