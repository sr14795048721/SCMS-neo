package com.scms.core.adminuser;

import com.scms.core.adminuser.dto.AdminManagerCreateRequest;
import com.scms.core.adminuser.dto.AdminManagerUpdateRequest;
import com.scms.core.adminuser.dto.AdminStudentImportResponse;
import com.scms.core.adminuser.dto.AdminStudentUpdateRequest;
import com.scms.core.adminuser.repository.AdminSortDirection;
import com.scms.core.adminuser.repository.AdminUserSortBy;
import com.scms.core.adminuser.repository.AdminUserQueryRepository;
import com.scms.core.adminuser.service.AdminStudentExcelService;
import com.scms.core.adminuser.service.AdminUserService;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.manager.service.ManagerAvatarStorageService;
import com.scms.core.notification.repository.NotificationRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.score.repository.ScoreRecordRepository;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.student.service.StudentAvatarStorageService;
import com.scms.core.telemetry.repository.VisitEventRepository;
import com.scms.core.telemetry.repository.VisitorSessionRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;

class AdminUserServiceTest {

    private AdminUserQueryRepository adminUserQueryRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private ManagerInfoRepository managerInfoRepository;
    private NotificationRepository notificationRepository;
    private RegistrationRepository registrationRepository;
    private ScoreRecordRepository scoreRecordRepository;
    private RewardOrderRepository rewardOrderRepository;
    private ClubJoinRequestRepository clubJoinRequestRepository;
    private AttendanceRecordRepository attendanceRecordRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ActivityRepository activityRepository;
    private VisitEventRepository visitEventRepository;
    private VisitorSessionRepository visitorSessionRepository;
    private StudentAvatarStorageService studentAvatarStorageService;
    private ManagerAvatarStorageService managerAvatarStorageService;
    private CurrentUserProvider currentUserProvider;
    private PasswordEncoder passwordEncoder;
    private NotificationService notificationService;
    private AuditService auditService;
    private PlatformTransactionManager transactionManager;
    private AdminStudentExcelService adminStudentExcelService;
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserQueryRepository = mock(AdminUserQueryRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        scoreRecordRepository = mock(ScoreRecordRepository.class);
        rewardOrderRepository = mock(RewardOrderRepository.class);
        clubJoinRequestRepository = mock(ClubJoinRequestRepository.class);
        attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        activityRepository = mock(ActivityRepository.class);
        visitEventRepository = mock(VisitEventRepository.class);
        visitorSessionRepository = mock(VisitorSessionRepository.class);
        studentAvatarStorageService = mock(StudentAvatarStorageService.class);
        managerAvatarStorageService = mock(ManagerAvatarStorageService.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        notificationService = mock(NotificationService.class);
        auditService = mock(AuditService.class);
        transactionManager = mock(PlatformTransactionManager.class);
        adminStudentExcelService = new AdminStudentExcelService();
        adminUserService = new AdminUserService(
                adminUserQueryRepository,
                userRepository,
                studentInfoRepository,
                managerInfoRepository,
                notificationRepository,
                registrationRepository,
                scoreRecordRepository,
                rewardOrderRepository,
                clubJoinRequestRepository,
                attendanceRecordRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                activityRepository,
                visitEventRepository,
                visitorSessionRepository,
                studentAvatarStorageService,
                managerAvatarStorageService,
                currentUserProvider,
                passwordEncoder,
                notificationService,
                auditService,
                adminStudentExcelService,
                transactionManager
        );

        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void updateStudentShouldUpdateUserAndProfile() {
        UserEntity studentUser = new UserEntity();
        setEntityId(studentUser, 11L);
        studentUser.setUsername("student-old");
        studentUser.setEmail("student-old@scms.local");
        studentUser.setRole(UserRole.STUDENT);
        studentUser.setEnabled(true);

        when(userRepository.findByIdAndRole(11L, UserRole.STUDENT)).thenReturn(Optional.of(studentUser));
        when(userRepository.existsByUsernameAndIdNot("student-new", 11L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("student-new@scms.local", 11L)).thenReturn(false);
        when(studentInfoRepository.findByUserId(11L)).thenReturn(Optional.empty());
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminUserQueryRepository.findStudentByUserId(UserRole.STUDENT.name(), 11L)).thenReturn(Optional.of(
                new com.scms.core.adminuser.repository.AdminStudentRow(
                        11L,
                        "student-new",
                        "student-new@scms.local",
                        false,
                        "学生新名字",
                        "20260001",
                        "HIGH_2",
                        "5",
                        "13800000000",
                        "新的简介",
                        false,
                        null,
                        "STUDENT",
                        null,
                        null
                )
        ));

        var response = adminUserService.updateStudent(11L, new AdminStudentUpdateRequest(
                "student-new",
                "student-new@scms.local",
                false,
                "学生新名字",
                "20260001",
                "HIGH_2",
                "5",
                "13800000000",
                "新的简介"
        ));

        assertEquals("student-new", response.username());
        assertEquals("student-new@scms.local", response.email());
        assertFalse(response.enabled());
        assertEquals("HIGH_2", response.grade());
        assertEquals("5", response.className());
        verify(auditService).create(eq("ADMIN_STUDENT_UPDATED"), eq(1L), eq("USER"), eq("11"), any(String.class));
        verify(notificationService).create(11L, "ACCOUNT", "账号信息已更新", "管理员更新了你的学生账号或资料信息。");
    }

    @Test
    void listStudentsShouldPassSortParametersToRepository() {
        when(adminUserQueryRepository.findStudents(
                UserRole.STUDENT.name(),
                "student",
                1,
                10,
                AdminUserSortBy.DISPLAY_NAME,
                AdminSortDirection.DESC
        )).thenReturn(new com.scms.core.adminuser.repository.AdminUserPageResult<>(List.of(), 0));

        var response = adminUserService.listStudents(1, 10, "student", "displayName", "desc");

        assertEquals(0, response.total());
        verify(adminUserQueryRepository).findStudents(
                UserRole.STUDENT.name(),
                "student",
                1,
                10,
                AdminUserSortBy.DISPLAY_NAME,
                AdminSortDirection.DESC
        );
    }

    @Test
    void listManagersShouldFallBackToDefaultSortWhenSortByInvalid() {
        when(adminUserQueryRepository.findManagers(
                UserRole.CLUB_MANAGER.name(),
                "",
                1,
                10,
                null,
                AdminSortDirection.ASC
        )).thenReturn(new com.scms.core.adminuser.repository.AdminUserPageResult<>(List.of(), 0));

        var response = adminUserService.listManagers(1, 10, "", "unknown", "asc");

        assertEquals(0, response.total());
        verify(adminUserQueryRepository).findManagers(
                UserRole.CLUB_MANAGER.name(),
                "",
                1,
                10,
                null,
                AdminSortDirection.ASC
        );
    }

    @Test
    void importStudentsShouldReturnPartialFailuresWithReasons() throws IOException {
        when(userRepository.existsByEmail("student-a@scms.local")).thenReturn(false);
        when(userRepository.existsByUsername("student-a")).thenReturn(false);
        when(passwordEncoder.encode("student123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            setEntityId(user, 21L);
            return user;
        });
        when(studentInfoRepository.saveAndFlush(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = buildImportFile(new String[][]{
                {"student-a", "student-a@scms.local", "张三", "20260001", "高一", "3", "13800000000", "简介A", "是"},
                {"student-b", "student-a@scms.local", "李四", "20260002", "高二", "4", "13900000000", "简介B", "是"}
        });

        AdminStudentImportResponse response = adminUserService.importStudents(file);

        assertEquals(2, response.totalRows());
        assertEquals(1, response.successCount());
        assertEquals(1, response.failureCount());
        assertEquals("DUPLICATE_EMAIL_IN_FILE", response.failures().get(0).reasonCode());
        verify(notificationService).create(21L, "ACCOUNT", "账号已创建", "管理员已导入你的学生账号，初始密码为 student123。");
    }

    @Test
    void resetManagerPasswordsShouldUpdateAllManagers() {
        UserEntity first = new UserEntity();
        setEntityId(first, 31L);
        first.setRole(UserRole.CLUB_MANAGER);
        UserEntity second = new UserEntity();
        setEntityId(second, 32L);
        second.setRole(UserRole.CLUB_MANAGER);

        when(userRepository.findAllByRole(UserRole.CLUB_MANAGER)).thenReturn(List.of(first, second));
        when(passwordEncoder.encode("teacher123")).thenReturn("encoded-teacher");

        var response = adminUserService.resetManagerPasswords();

        assertEquals(2, response.resetCount());
        assertEquals("teacher123", response.defaultPassword());
        assertEquals("encoded-teacher", first.getPasswordHash());
        assertEquals("encoded-teacher", second.getPasswordHash());
        verify(notificationService, times(2)).create(any(Long.class), eq("SECURITY"), eq("密码已重置"), eq("管理员已将你的密码重置为 teacher123。"));
        verify(auditService).create(eq("ADMIN_MANAGERS_PASSWORD_RESET"), eq(1L), eq("USER_ROLE"), eq("CLUB_MANAGER"), any(String.class));
    }

    @Test
    void createManagerShouldCreateUserAndProfileWithDefaultPassword() {
        when(userRepository.existsByUsername("teacher-new")).thenReturn(false);
        when(userRepository.existsByEmail("teacher-new@scms.local")).thenReturn(false);
        when(passwordEncoder.encode("teacher123")).thenReturn("encoded-teacher");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            setEntityId(user, 35L);
            return user;
        });
        when(managerInfoRepository.saveAndFlush(any(ManagerInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminUserQueryRepository.findManagerByUserId(UserRole.CLUB_MANAGER.name(), 35L)).thenReturn(Optional.of(
                new com.scms.core.adminuser.repository.AdminManagerRow(
                        35L,
                        "teacher-new",
                        "teacher-new@scms.local",
                        true,
                        "Teacher New",
                        "T-1002",
                        "13600000000",
                        "New teacher bio",
                        false,
                        null,
                        "CLUB_MANAGER",
                        null,
                        null
                )
        ));

        var response = adminUserService.createManager(new AdminManagerCreateRequest(
                "teacher-new",
                "teacher-new@scms.local",
                true,
                "Teacher New",
                "T-1002",
                "13600000000",
                "New teacher bio"
        ));

        assertEquals("teacher123", response.defaultPassword());
        assertEquals("teacher-new", response.manager().username());
        assertEquals("teacher-new@scms.local", response.manager().email());
        assertTrue(response.manager().enabled());
        verify(userRepository).saveAndFlush(any(UserEntity.class));
        verify(managerInfoRepository).saveAndFlush(any(ManagerInfoEntity.class));
        verify(notificationService).create(
                35L,
                "ACCOUNT",
                "Account created",
                "An administrator created your teacher account. Your initial password is teacher123."
        );
        verify(auditService).create(eq("ADMIN_MANAGER_CREATED"), eq(1L), eq("USER"), eq("35"), any(String.class));
    }

    @Test
    void updateManagerShouldUpdateUserAndProfile() {
        UserEntity managerUser = new UserEntity();
        setEntityId(managerUser, 15L);
        managerUser.setUsername("teacher-old");
        managerUser.setEmail("teacher-old@scms.local");
        managerUser.setRole(UserRole.CLUB_MANAGER);
        managerUser.setEnabled(true);

        when(userRepository.findByIdAndRole(15L, UserRole.CLUB_MANAGER)).thenReturn(Optional.of(managerUser));
        when(userRepository.existsByUsernameAndIdNot("teacher-new", 15L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("teacher-new@scms.local", 15L)).thenReturn(false);
        when(managerInfoRepository.findByUserId(15L)).thenReturn(Optional.of(new ManagerInfoEntity()));
        when(managerInfoRepository.save(any(ManagerInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminUserQueryRepository.findManagerByUserId(UserRole.CLUB_MANAGER.name(), 15L)).thenReturn(Optional.of(
                new com.scms.core.adminuser.repository.AdminManagerRow(
                        15L,
                        "teacher-new",
                        "teacher-new@scms.local",
                        true,
                        "教师新名字",
                        "T-1001",
                        "13700000000",
                        "教师简介",
                        false,
                        null,
                        "CLUB_MANAGER",
                        null,
                        null
                )
        ));

        var response = adminUserService.updateManager(15L, new AdminManagerUpdateRequest(
                "teacher-new",
                "teacher-new@scms.local",
                true,
                "教师新名字",
                "T-1001",
                "13700000000",
                "教师简介"
        ));

        assertEquals("teacher-new", response.username());
        assertEquals("teacher-new@scms.local", response.email());
        assertTrue(response.enabled());
        verify(notificationService).create(15L, "ACCOUNT", "账号信息已更新", "管理员更新了你的教师账号或资料信息。");
    }

    @Test
    void deleteStudentShouldRemoveProfileRelationsAndUser() {
        UserEntity studentUser = new UserEntity();
        setEntityId(studentUser, 41L);
        studentUser.setRole(UserRole.STUDENT);

        StudentInfoEntity info = new StudentInfoEntity();
        info.setUserId(41L);
        info.setAvatarPath("student-avatar.png");

        when(userRepository.findByIdAndRole(41L, UserRole.STUDENT)).thenReturn(Optional.of(studentUser));
        when(studentInfoRepository.findByUserId(41L)).thenReturn(Optional.of(info));

        var response = adminUserService.deleteStudent(41L);

        assertEquals(41L, response.deletedUserId());
        verify(registrationRepository).deleteAllByUserId(41L);
        verify(scoreRecordRepository).deleteAllByUserId(41L);
        verify(rewardOrderRepository).deleteAllByUserId(41L);
        verify(clubJoinRequestRepository).deleteAllByStudentUserId(41L);
        verify(clubStudentMemberRepository).deleteAllByStudentUserId(41L);
        verify(notificationRepository).deleteAllByUserId(41L);
        verify(visitEventRepository).clearUserReferenceByUserId(41L);
        verify(visitorSessionRepository).clearUserReferenceByUserId(41L);
        verify(studentInfoRepository).delete(info);
        verify(userRepository).delete(studentUser);
        verify(studentAvatarStorageService).deleteIfExists("student-avatar.png");
        verify(auditService).create(eq("ADMIN_STUDENT_DELETED"), eq(1L), eq("USER"), eq("41"), any(String.class));
    }

    @Test
    void resetSingleStudentPasswordShouldOnlyUpdateSelectedStudent() {
        UserEntity studentUser = new UserEntity();
        setEntityId(studentUser, 42L);
        studentUser.setRole(UserRole.STUDENT);

        when(userRepository.findByIdAndRole(42L, UserRole.STUDENT)).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.encode("student123")).thenReturn("encoded-student");

        var response = adminUserService.resetStudentPassword(42L);

        assertEquals(1, response.resetCount());
        assertEquals("student123", response.defaultPassword());
        assertEquals("encoded-student", studentUser.getPasswordHash());
        verify(userRepository).save(studentUser);
        verify(notificationService).create(42L, "SECURITY", "密码已重置", "管理员已将你的密码重置为 student123。");
        verify(auditService).create(eq("ADMIN_STUDENT_PASSWORD_RESET"), eq(1L), eq("USER"), eq("42"), any(String.class));
    }

    @Test
    void deleteManagerShouldBlockWhenDependenciesExist() {
        UserEntity managerUser = new UserEntity();
        setEntityId(managerUser, 51L);
        managerUser.setRole(UserRole.CLUB_MANAGER);

        when(userRepository.findByIdAndRole(51L, UserRole.CLUB_MANAGER)).thenReturn(Optional.of(managerUser));
        when(clubRepository.countByCreatedBy(51L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminUserService.deleteManager(51L));

        assertEquals(ErrorCode.DEPENDENCY_EXISTS, exception.getErrorCode());
        verify(userRepository, times(0)).delete(any(UserEntity.class));
        verify(managerAvatarStorageService, times(0)).deleteIfExists(any(String.class));
        verify(clubManagerBindingRepository, times(0)).deleteAllByManagerUserId(any(Long.class));
    }

    @Test
    void deleteManagerShouldClearTelemetryReferencesBeforeDeletingUser() {
        UserEntity managerUser = new UserEntity();
        setEntityId(managerUser, 61L);
        managerUser.setRole(UserRole.CLUB_MANAGER);

        ManagerInfoEntity info = new ManagerInfoEntity();
        info.setUserId(61L);
        info.setAvatarPath("manager-avatar.png");

        when(userRepository.findByIdAndRole(61L, UserRole.CLUB_MANAGER)).thenReturn(Optional.of(managerUser));
        when(clubRepository.countByCreatedBy(61L)).thenReturn(0L);
        when(activityRepository.countByCreatedBy(61L)).thenReturn(0L);
        when(managerInfoRepository.findByUserId(61L)).thenReturn(Optional.of(info));

        var response = adminUserService.deleteManager(61L);

        assertEquals(61L, response.deletedUserId());
        verify(registrationRepository).deleteAllByUserId(61L);
        verify(clubManagerBindingRepository).deleteAllByManagerUserId(61L);
        verify(notificationRepository).deleteAllByUserId(61L);
        verify(visitEventRepository).clearUserReferenceByUserId(61L);
        verify(visitorSessionRepository).clearUserReferenceByUserId(61L);
        verify(managerInfoRepository).delete(info);
        verify(userRepository).delete(managerUser);
        verify(managerAvatarStorageService).deleteIfExists("manager-avatar.png");
    }

    private MockMultipartFile buildImportFile(String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("students");
            var header = sheet.createRow(0);
            String[] headers = {"用户名", "邮箱", "姓名", "学号", "年级", "班级", "手机号", "个人简介", "是否启用"};
            for (int index = 0; index < headers.length; index += 1) {
                header.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex += 1) {
                var row = sheet.createRow(rowIndex + 1);
                for (int cellIndex = 0; cellIndex < rows[rowIndex].length; cellIndex += 1) {
                    row.createCell(cellIndex).setCellValue(rows[rowIndex][cellIndex]);
                }
            }
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }

    private void setEntityId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
