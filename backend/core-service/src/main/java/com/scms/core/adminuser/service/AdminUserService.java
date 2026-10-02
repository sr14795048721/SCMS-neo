package com.scms.core.adminuser.service;

import com.scms.core.adminuser.dto.AdminBulkResetPasswordResponse;
import com.scms.core.adminuser.dto.AdminDeleteUserResponse;
import com.scms.core.adminuser.dto.AdminManagerCreateRequest;
import com.scms.core.adminuser.dto.AdminManagerCreateResponse;
import com.scms.core.adminuser.dto.AdminManagerDetailResponse;
import com.scms.core.adminuser.dto.AdminManagerListItemResponse;
import com.scms.core.adminuser.dto.AdminManagerOptionResponse;
import com.scms.core.adminuser.dto.AdminManagerUpdateRequest;
import com.scms.core.adminuser.dto.AdminStudentDetailResponse;
import com.scms.core.adminuser.dto.AdminStudentImportFailureResponse;
import com.scms.core.adminuser.dto.AdminStudentImportResponse;
import com.scms.core.adminuser.dto.AdminStudentListItemResponse;
import com.scms.core.adminuser.dto.AdminStudentUpdateRequest;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.adminuser.repository.AdminManagerRow;
import com.scms.core.adminuser.repository.AdminStudentRow;
import com.scms.core.adminuser.repository.AdminUserPageResult;
import com.scms.core.adminuser.repository.AdminUserQueryRepository;
import com.scms.core.adminuser.repository.AdminUserSortBy;
import com.scms.core.adminuser.repository.AdminSortDirection;
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
import com.scms.core.student.domain.StudentGrade;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.student.service.StudentAvatarStorageService;
import com.scms.core.telemetry.repository.VisitEventRepository;
import com.scms.core.telemetry.repository.VisitorSessionRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_STUDENT_PASSWORD = "student123";
    private static final String DEFAULT_MANAGER_PASSWORD = "teacher123";

    private final AdminUserQueryRepository adminUserQueryRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final NotificationRepository notificationRepository;
    private final RegistrationRepository registrationRepository;
    private final ScoreRecordRepository scoreRecordRepository;
    private final RewardOrderRepository rewardOrderRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ActivityRepository activityRepository;
    private final VisitEventRepository visitEventRepository;
    private final VisitorSessionRepository visitorSessionRepository;
    private final StudentAvatarStorageService studentAvatarStorageService;
    private final ManagerAvatarStorageService managerAvatarStorageService;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AdminStudentExcelService adminStudentExcelService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public AdminUserService(AdminUserQueryRepository adminUserQueryRepository,
                            UserRepository userRepository,
                            StudentInfoRepository studentInfoRepository,
                            ManagerInfoRepository managerInfoRepository,
                            NotificationRepository notificationRepository,
                            RegistrationRepository registrationRepository,
                            ScoreRecordRepository scoreRecordRepository,
                            RewardOrderRepository rewardOrderRepository,
                            ClubJoinRequestRepository clubJoinRequestRepository,
                            AttendanceRecordRepository attendanceRecordRepository,
                            ClubRepository clubRepository,
                            ClubManagerBindingRepository clubManagerBindingRepository,
                            ClubStudentMemberRepository clubStudentMemberRepository,
                            ActivityRepository activityRepository,
                            VisitEventRepository visitEventRepository,
                            VisitorSessionRepository visitorSessionRepository,
                            StudentAvatarStorageService studentAvatarStorageService,
                            ManagerAvatarStorageService managerAvatarStorageService,
                            CurrentUserProvider currentUserProvider,
                            PasswordEncoder passwordEncoder,
                            NotificationService notificationService,
                            AuditService auditService,
                            AdminStudentExcelService adminStudentExcelService,
                            PlatformTransactionManager transactionManager) {
        this.adminUserQueryRepository = adminUserQueryRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.notificationRepository = notificationRepository;
        this.registrationRepository = registrationRepository;
        this.scoreRecordRepository = scoreRecordRepository;
        this.rewardOrderRepository = rewardOrderRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.activityRepository = activityRepository;
        this.visitEventRepository = visitEventRepository;
        this.visitorSessionRepository = visitorSessionRepository;
        this.studentAvatarStorageService = studentAvatarStorageService;
        this.managerAvatarStorageService = managerAvatarStorageService;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.adminStudentExcelService = adminStudentExcelService;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public AdminUserPageResponse<AdminStudentListItemResponse> listStudents(int page,
                                                                            int pageSize,
                                                                            String keyword,
                                                                            String sortBy,
                                                                            String sortDirection) {
        requireAdmin();
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        AdminUserSortBy normalizedSortBy = AdminUserSortBy.fromValue(sortBy);
        AdminSortDirection normalizedSortDirection = AdminSortDirection.fromValue(sortDirection);
        AdminUserPageResult<AdminStudentRow> result = adminUserQueryRepository.findStudents(
                UserRole.STUDENT.name(),
                normalizeKeyword(keyword),
                normalizedPage,
                normalizedPageSize,
                normalizedSortBy,
                normalizedSortDirection
        );

        return new AdminUserPageResponse<>(
                result.items().stream().map(this::toStudentListItem).toList(),
                normalizedPage,
                normalizedPageSize,
                result.total(),
                calculateTotalPages(result.total(), normalizedPageSize)
        );
    }

    public AdminStudentDetailResponse getStudentDetail(Long userId) {
        requireAdmin();
        return adminUserQueryRepository.findStudentByUserId(UserRole.STUDENT.name(), userId)
                .map(this::toStudentDetail)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学生账号不存在"));
    }

    public AdminStudentDetailResponse updateStudent(Long userId, AdminStudentUpdateRequest request) {
        AuthenticatedUser admin = requireAdmin();
        UserEntity user = userRepository.findByIdAndRole(userId, UserRole.STUDENT)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学生账号不存在"));

        String normalizedUsername = sanitizeUsername(request.username());
        String normalizedEmail = normalizeEmail(request.email());
        validateUniqueUserFields(userId, normalizedUsername, normalizedEmail);

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setEnabled(request.enabled());
        userRepository.save(user);

        StudentInfoEntity info = studentInfoRepository.findByUserId(userId).orElseGet(() -> createEmptyStudentInfo(userId));
        info.setDisplayName(trimToNull(request.displayName()));
        info.setStudentNo(trimToNull(request.studentNo()));
        info.setGrade(normalizeStudentGrade(request.grade(), true));
        info.setClassName(normalizeStudentClassName(request.className(), true));
        info.setPhone(trimToNull(request.phone()));
        info.setBio(trimToNull(request.bio()));
        studentInfoRepository.save(info);

        auditService.create(
                "ADMIN_STUDENT_UPDATED",
                admin.userId(),
                "USER",
                String.valueOf(userId),
                "管理员更新了学生账号信息"
        );
        notificationService.create(userId, "ACCOUNT", "账号信息已更新", "管理员更新了你的学生账号或资料信息。");

        return getStudentDetail(userId);
    }

    public AdminStudentImportResponse importStudents(MultipartFile file) {
        AuthenticatedUser admin = requireAdmin();
        List<AdminStudentExcelService.AdminStudentImportRow> rows = adminStudentExcelService.parseImport(file);
        List<AdminStudentImportFailureResponse> failures = new ArrayList<>();
        List<String> fileEmails = new ArrayList<>();
        List<String> fileUsernames = new ArrayList<>();
        int successCount = 0;

        for (AdminStudentExcelService.AdminStudentImportRow row : rows) {
            String normalizedEmail = normalizeEmail(row.email());
            String normalizedUsername = sanitizeUsernameValue(row.username());

            AdminStudentImportFailureResponse precheckFailure = validateImportRow(
                    row,
                    normalizedUsername,
                    normalizedEmail,
                    fileUsernames,
                    fileEmails
            );
            if (precheckFailure != null) {
                failures.add(precheckFailure);
                continue;
            }

            fileUsernames.add(normalizedUsername);
            fileEmails.add(normalizedEmail);

            try {
                requiresNewTransactionTemplate.executeWithoutResult(status ->
                        createImportedStudent(row, normalizedUsername, normalizedEmail)
                );
                successCount += 1;
            } catch (BusinessException exception) {
                failures.add(new AdminStudentImportFailureResponse(
                        row.rowNumber(),
                        normalizedEmail,
                        exception.getErrorCode().name(),
                        exception.getMessage()
                ));
            } catch (DataIntegrityViolationException exception) {
                failures.add(new AdminStudentImportFailureResponse(
                        row.rowNumber(),
                        normalizedEmail,
                        ErrorCode.CONFLICT.name(),
                        "用户名或邮箱已存在"
                ));
            } catch (RuntimeException exception) {
                failures.add(new AdminStudentImportFailureResponse(
                        row.rowNumber(),
                        normalizedEmail,
                        ErrorCode.SYSTEM_ERROR.name(),
                        "导入该行数据时发生异常"
                ));
            }
        }

        auditService.create(
                "ADMIN_STUDENTS_IMPORTED",
                admin.userId(),
                "STUDENT_IMPORT",
                null,
                "管理员批量导入学生，成功 " + successCount + " 条，失败 " + failures.size() + " 条"
        );

        return new AdminStudentImportResponse(rows.size(), successCount, failures.size(), failures);
    }

    public AdminUserExcelFileContent exportStudents(String keyword) {
        requireAdmin();
        List<AdminStudentExcelService.AdminStudentExportRow> rows = adminUserQueryRepository
                .findStudentsForExport(UserRole.STUDENT.name(), normalizeKeyword(keyword))
                .stream()
                .map(row -> new AdminStudentExcelService.AdminStudentExportRow(
                        row.username(),
                        row.email(),
                        row.displayName(),
                        row.studentNo(),
                        resolveStudentGradeLabel(row.grade()),
                        row.className(),
                        row.phone(),
                        row.bio(),
                        row.enabled() ? "是" : "否"
                ))
                .toList();
        return adminStudentExcelService.buildExport(rows);
    }

    public AdminUserExcelFileContent downloadStudentTemplate() {
        requireAdmin();
        return adminStudentExcelService.buildTemplate();
    }

    public AdminBulkResetPasswordResponse resetStudentPasswords() {
        AuthenticatedUser admin = requireAdmin();
        List<UserEntity> users = userRepository.findAllByRole(UserRole.STUDENT);
        String encodedPassword = passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD);
        for (UserEntity user : users) {
            user.setPasswordHash(encodedPassword);
        }
        userRepository.saveAll(users);
        for (UserEntity user : users) {
            notificationService.create(user.getId(), "SECURITY", "密码已重置", "管理员已将你的密码重置为 student123。");
        }
        auditService.create(
                "ADMIN_STUDENTS_PASSWORD_RESET",
                admin.userId(),
                "USER_ROLE",
                UserRole.STUDENT.name(),
                "管理员批量重置了全部学生密码"
        );
        return new AdminBulkResetPasswordResponse(users.size(), DEFAULT_STUDENT_PASSWORD);
    }

    public AdminBulkResetPasswordResponse resetStudentPassword(Long userId) {
        AuthenticatedUser admin = requireAdmin();
        UserEntity user = userRepository.findByIdAndRole(userId, UserRole.STUDENT)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "student account not found"));

        user.setPasswordHash(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD));
        userRepository.save(user);

        notificationService.create(userId, "SECURITY", "密码已重置", "管理员已将你的密码重置为 student123。");
        auditService.create(
                "ADMIN_STUDENT_PASSWORD_RESET",
                admin.userId(),
                "USER",
                String.valueOf(userId),
                "Administrator reset a student password"
        );
        return new AdminBulkResetPasswordResponse(1, DEFAULT_STUDENT_PASSWORD);
    }

    @Transactional
    public AdminDeleteUserResponse deleteStudent(Long userId) {
        AuthenticatedUser admin = requireAdmin();
        UserEntity user = userRepository.findByIdAndRole(userId, UserRole.STUDENT)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "student account not found"));

        StudentInfoEntity info = studentInfoRepository.findByUserId(userId).orElse(null);
        String avatarPath = info == null ? null : info.getAvatarPath();

        registrationRepository.deleteAllByUserId(userId);
        scoreRecordRepository.deleteAllByUserId(userId);
        rewardOrderRepository.deleteAllByUserId(userId);
        clubJoinRequestRepository.deleteAllByStudentUserId(userId);
        attendanceRecordRepository.deleteAllByStudentUserId(userId);
        clubStudentMemberRepository.deleteAllByStudentUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        clearTelemetryUserReferences(userId);
        if (info != null) {
            studentInfoRepository.delete(info);
        }
        userRepository.delete(user);
        studentAvatarStorageService.deleteIfExists(avatarPath);

        auditService.create(
                "ADMIN_STUDENT_DELETED",
                admin.userId(),
                "USER",
                String.valueOf(userId),
                "Administrator deleted a student account"
        );
        return new AdminDeleteUserResponse(userId);
    }

    public AdminUserPageResponse<AdminManagerListItemResponse> listManagers(int page,
                                                                            int pageSize,
                                                                            String keyword,
                                                                            String sortBy,
                                                                            String sortDirection) {
        requireAdmin();
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        AdminUserSortBy normalizedSortBy = AdminUserSortBy.fromValue(sortBy);
        AdminSortDirection normalizedSortDirection = AdminSortDirection.fromValue(sortDirection);
        AdminUserPageResult<AdminManagerRow> result = adminUserQueryRepository.findManagers(
                UserRole.CLUB_MANAGER.name(),
                normalizeKeyword(keyword),
                normalizedPage,
                normalizedPageSize,
                normalizedSortBy,
                normalizedSortDirection
        );
        return new AdminUserPageResponse<>(
                result.items().stream().map(this::toManagerListItem).toList(),
                normalizedPage,
                normalizedPageSize,
                result.total(),
                calculateTotalPages(result.total(), normalizedPageSize)
        );
    }

    @Transactional
    public AdminManagerCreateResponse createManager(AdminManagerCreateRequest request) {
        AuthenticatedUser admin = requireAdmin();

        String normalizedUsername = sanitizeUsername(request.username());
        String normalizedEmail = normalizeEmail(request.email());
        validateUniqueUserFields(normalizedUsername, normalizedEmail);

        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_MANAGER_PASSWORD));
        user.setRole(UserRole.CLUB_MANAGER);
        user.setEnabled(request.enabled());
        UserEntity savedUser = userRepository.saveAndFlush(user);

        ManagerInfoEntity info = createEmptyManagerInfo(savedUser.getId());
        info.setDisplayName(trimToNull(request.displayName()));
        info.setManagerNo(trimToNull(request.managerNo()));
        info.setPhone(trimToNull(request.phone()));
        info.setBio(trimToNull(request.bio()));
        managerInfoRepository.saveAndFlush(info);

        auditService.create(
                "ADMIN_MANAGER_CREATED",
                admin.userId(),
                "USER",
                String.valueOf(savedUser.getId()),
                "Administrator created a manager account"
        );
        notificationService.create(
                savedUser.getId(),
                "ACCOUNT",
                "Account created",
                "An administrator created your teacher account. Your initial password is " + DEFAULT_MANAGER_PASSWORD + "."
        );

        return new AdminManagerCreateResponse(getManagerDetail(savedUser.getId()), DEFAULT_MANAGER_PASSWORD);
    }

    public AdminManagerDetailResponse getManagerDetail(Long userId) {
        requireAdmin();
        return adminUserQueryRepository.findManagerByUserId(UserRole.CLUB_MANAGER.name(), userId)
                .map(this::toManagerDetail)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "教师账号不存在"));
    }

    public List<AdminManagerOptionResponse> listManagerOptions(String keyword) {
        requireAdmin();
        String normalizedKeyword = normalizeKeyword(keyword);

        return userRepository.findAllByRole(UserRole.CLUB_MANAGER)
                .stream()
                .map(user -> {
                    ManagerInfoEntity info = managerInfoRepository.findByUserId(user.getId()).orElse(null);
                    String displayName = info != null && trimToNull(info.getDisplayName()) != null
                            ? info.getDisplayName()
                            : user.getUsername();
                    String managerNo = info == null ? "" : defaultString(info.getManagerNo());
                    return new AdminManagerOptionResponse(user.getId(), user.getUsername(), displayName, managerNo);
                })
                .filter(item -> normalizedKeyword.isEmpty() || matchesManagerOptionKeyword(item, normalizedKeyword))
                .sorted((left, right) -> (left.displayName() + left.username()).compareToIgnoreCase(right.displayName() + right.username()))
                .limit(20)
                .toList();
    }

    public AdminManagerDetailResponse updateManager(Long userId, AdminManagerUpdateRequest request) {
        AuthenticatedUser admin = requireAdmin();
        UserEntity user = userRepository.findByIdAndRole(userId, UserRole.CLUB_MANAGER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "教师账号不存在"));

        String normalizedUsername = sanitizeUsername(request.username());
        String normalizedEmail = normalizeEmail(request.email());
        validateUniqueUserFields(userId, normalizedUsername, normalizedEmail);

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setEnabled(request.enabled());
        userRepository.save(user);

        ManagerInfoEntity info = managerInfoRepository.findByUserId(userId).orElseGet(() -> createEmptyManagerInfo(userId));
        info.setDisplayName(trimToNull(request.displayName()));
        info.setManagerNo(trimToNull(request.managerNo()));
        info.setPhone(trimToNull(request.phone()));
        info.setBio(trimToNull(request.bio()));
        managerInfoRepository.save(info);

        auditService.create(
                "ADMIN_MANAGER_UPDATED",
                admin.userId(),
                "USER",
                String.valueOf(userId),
                "管理员更新了教师账号信息"
        );
        notificationService.create(userId, "ACCOUNT", "账号信息已更新", "管理员更新了你的教师账号或资料信息。");

        return getManagerDetail(userId);
    }

    public AdminBulkResetPasswordResponse resetManagerPasswords() {
        AuthenticatedUser admin = requireAdmin();
        List<UserEntity> users = userRepository.findAllByRole(UserRole.CLUB_MANAGER);
        String encodedPassword = passwordEncoder.encode(DEFAULT_MANAGER_PASSWORD);
        for (UserEntity user : users) {
            user.setPasswordHash(encodedPassword);
        }
        userRepository.saveAll(users);
        for (UserEntity user : users) {
            notificationService.create(user.getId(), "SECURITY", "密码已重置", "管理员已将你的密码重置为 teacher123。");
        }
        auditService.create(
                "ADMIN_MANAGERS_PASSWORD_RESET",
                admin.userId(),
                "USER_ROLE",
                UserRole.CLUB_MANAGER.name(),
                "管理员批量重置了全部教师密码"
        );
        return new AdminBulkResetPasswordResponse(users.size(), DEFAULT_MANAGER_PASSWORD);
    }

    @Transactional
    public AdminDeleteUserResponse deleteManager(Long userId) {
        AuthenticatedUser admin = requireAdmin();
        UserEntity user = userRepository.findByIdAndRole(userId, UserRole.CLUB_MANAGER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "manager account not found"));

        if (clubRepository.countByCreatedBy(userId) > 0 || activityRepository.countByCreatedBy(userId) > 0) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS, "manager account still owns clubs or activities");
        }

        ManagerInfoEntity info = managerInfoRepository.findByUserId(userId).orElse(null);
        String avatarPath = info == null ? null : info.getAvatarPath();

        registrationRepository.deleteAllByUserId(userId);
        clubManagerBindingRepository.deleteAllByManagerUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        clearTelemetryUserReferences(userId);
        if (info != null) {
            managerInfoRepository.delete(info);
        }
        userRepository.delete(user);
        managerAvatarStorageService.deleteIfExists(avatarPath);

        auditService.create(
                "ADMIN_MANAGER_DELETED",
                admin.userId(),
                "USER",
                String.valueOf(userId),
                "Administrator deleted a manager account"
        );
        return new AdminDeleteUserResponse(userId);
    }

    private void createImportedStudent(AdminStudentExcelService.AdminStudentImportRow row,
                                       String normalizedUsername,
                                       String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD));
        user.setRole(UserRole.STUDENT);
        user.setEnabled(resolveImportEnabled(row.enabled()));
        UserEntity savedUser = userRepository.saveAndFlush(user);

        StudentInfoEntity info = createEmptyStudentInfo(savedUser.getId());
        info.setDisplayName(trimToNull(row.displayName()));
        info.setStudentNo(trimToNull(row.studentNo()));
        info.setGrade(normalizeStudentGrade(row.grade(), false));
        info.setClassName(normalizeStudentClassName(row.className(), false));
        info.setPhone(trimToNull(row.phone()));
        info.setBio(trimToNull(row.bio()));
        studentInfoRepository.saveAndFlush(info);

        notificationService.create(savedUser.getId(), "ACCOUNT", "账号已创建", "管理员已导入你的学生账号，初始密码为 student123。");
    }

    private AdminStudentImportFailureResponse validateImportRow(
            AdminStudentExcelService.AdminStudentImportRow row,
            String normalizedUsername,
            String normalizedEmail,
            List<String> fileUsernames,
            List<String> fileEmails
    ) {
        if (normalizedUsername.isBlank()) {
            return failure(row, normalizedEmail, "REQUIRED_USERNAME", "用户名不能为空");
        }
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 64) {
            return failure(row, normalizedEmail, "INVALID_USERNAME", "用户名长度必须在 3 到 64 之间");
        }
        if (normalizedEmail.isBlank()) {
            return failure(row, normalizedEmail, "REQUIRED_EMAIL", "邮箱不能为空");
        }
        if (!isValidEmail(normalizedEmail)) {
            return failure(row, normalizedEmail, "INVALID_EMAIL", "邮箱格式不正确");
        }
        if (fileEmails.contains(normalizedEmail)) {
            return failure(row, normalizedEmail, "DUPLICATE_EMAIL_IN_FILE", "导入文件内存在重复邮箱");
        }
        if (fileUsernames.contains(normalizedUsername)) {
            return failure(row, normalizedEmail, "DUPLICATE_USERNAME_IN_FILE", "导入文件内存在重复用户名");
        }
        String displayName = trimToNull(row.displayName());
        if (displayName == null) {
            return failure(row, normalizedEmail, "REQUIRED_DISPLAY_NAME", "姓名不能为空");
        }
        if (displayName.length() > 120) {
            return failure(row, normalizedEmail, "INVALID_DISPLAY_NAME", "姓名长度不能超过 120");
        }
        String studentNo = trimToNull(row.studentNo());
        if (studentNo != null && studentNo.length() > 64) {
            return failure(row, normalizedEmail, "INVALID_STUDENT_NO", "学号长度不能超过 64");
        }
        if (normalizeStudentGrade(row.grade(), false) == null) {
            return failure(row, normalizedEmail, "INVALID_GRADE", "年级必须是高一、高二或高三");
        }
        if (trimToNull(row.className()) != null && normalizeStudentClassName(row.className(), false) == null) {
            return failure(row, normalizedEmail, "INVALID_CLASS_NAME", "班级只能填写 1 到 30 的数字");
        }
        String phone = trimToNull(row.phone());
        if (phone != null && phone.length() > 32) {
            return failure(row, normalizedEmail, "INVALID_PHONE", "手机号长度不能超过 32");
        }
        String bio = trimToNull(row.bio());
        if (bio != null && bio.length() > 1000) {
            return failure(row, normalizedEmail, "INVALID_BIO", "个人简介长度不能超过 1000");
        }
        if (!isValidEnabledValue(row.enabled())) {
            return failure(row, normalizedEmail, "INVALID_ENABLED", "是否启用只能填写是或否");
        }
        return null;
    }

    private AdminStudentImportFailureResponse failure(
            AdminStudentExcelService.AdminStudentImportRow row,
            String email,
            String reasonCode,
            String reasonMessage
    ) {
        return new AdminStudentImportFailureResponse(row.rowNumber(), email, reasonCode, reasonMessage);
    }

    private AdminStudentListItemResponse toStudentListItem(AdminStudentRow row) {
        return new AdminStudentListItemResponse(
                row.userId(),
                row.username(),
                row.email(),
                row.enabled(),
                row.displayName(),
                row.studentNo(),
                row.grade(),
                row.className(),
                row.phone(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private AdminStudentDetailResponse toStudentDetail(AdminStudentRow row) {
        return new AdminStudentDetailResponse(
                row.userId(),
                row.username(),
                row.email(),
                row.enabled(),
                row.displayName(),
                row.studentNo(),
                row.grade(),
                row.className(),
                row.phone(),
                row.bio(),
                row.role(),
                row.hasAvatar(),
                row.avatarUpdatedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private AdminManagerListItemResponse toManagerListItem(AdminManagerRow row) {
        return new AdminManagerListItemResponse(
                row.userId(),
                row.username(),
                row.email(),
                row.enabled(),
                row.displayName(),
                row.managerNo(),
                row.phone(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private AdminManagerDetailResponse toManagerDetail(AdminManagerRow row) {
        return new AdminManagerDetailResponse(
                row.userId(),
                row.username(),
                row.email(),
                row.enabled(),
                row.displayName(),
                row.managerNo(),
                row.phone(),
                row.bio(),
                row.role(),
                row.hasAvatar(),
                row.avatarUpdatedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private void clearTelemetryUserReferences(Long userId) {
        visitEventRepository.clearUserReferenceByUserId(userId);
        visitorSessionRepository.clearUserReferenceByUserId(userId);
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return currentUser;
    }

    private void validateUniqueUserFields(Long userId, String username, String email) {
        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }
    }

    private boolean matchesManagerOptionKeyword(AdminManagerOptionResponse item, String keyword) {
        String joined = String.join(
                " ",
                defaultString(item.username()).toLowerCase(Locale.ROOT),
                defaultString(item.displayName()).toLowerCase(Locale.ROOT),
                defaultString(item.managerNo()).toLowerCase(Locale.ROOT)
        );
        return joined.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private void validateUniqueUserFields(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.CONFLICT, "email already exists");
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private StudentInfoEntity createEmptyStudentInfo(Long userId) {
        StudentInfoEntity info = new StudentInfoEntity();
        info.setUserId(userId);
        return info;
    }

    private ManagerInfoEntity createEmptyManagerInfo(Long userId) {
        ManagerInfoEntity info = new ManagerInfoEntity();
        info.setUserId(userId);
        return info;
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int calculateTotalPages(long total, int pageSize) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / (double) pageSize);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String sanitizeUsername(String username) {
        String normalized = sanitizeUsernameValue(username);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户名不能为空");
        }
        return normalized;
    }

    private String sanitizeUsernameValue(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        if (normalized == null) {
            return "";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private String trimToNull(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeStudentGrade(String grade, boolean strict) {
        String normalized = StudentGrade.normalize(grade);
        if (normalized == null && strict) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "年级必须是 HIGH_1、HIGH_2 或 HIGH_3");
        }
        return normalized;
    }

    private String normalizeStudentClassName(String className, boolean strict) {
        String normalized = trimToNull(className);
        if (normalized == null) {
            return null;
        }

        String digits = normalized.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            if (strict) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "班级只能填写 1 到 30 的数字");
            }
            return null;
        }

        int classNumber = Integer.parseInt(digits);
        if (classNumber < 1 || classNumber > 30) {
            if (strict) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "班级只能填写 1 到 30 的数字");
            }
            return null;
        }
        return String.valueOf(classNumber);
    }

    private boolean isValidEnabledValue(String enabled) {
        String normalized = trimToNull(enabled);
        return normalized == null
                || "是".equals(normalized)
                || "否".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "false".equalsIgnoreCase(normalized)
                || "1".equals(normalized)
                || "0".equals(normalized);
    }

    private boolean resolveImportEnabled(String enabled) {
        String normalized = trimToNull(enabled);
        if (normalized == null) {
            return true;
        }
        return "是".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "1".equals(normalized);
    }

    private String resolveStudentGradeLabel(String grade) {
        return switch (grade == null ? "" : grade) {
            case "HIGH_1" -> "高一";
            case "HIGH_2" -> "高二";
            case "HIGH_3" -> "高三";
            default -> "";
        };
    }
}
