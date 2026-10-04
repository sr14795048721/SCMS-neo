package com.scms.core.student.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentGrade;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.dto.ChangeStudentPasswordRequest;
import com.scms.core.student.dto.StudentInfoResponse;
import com.scms.core.student.dto.UpdateStudentInfoRequest;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StudentService {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^(\\d{1,3})$");
    private static final Pattern LEGACY_CLASS_NAME_PATTERN = Pattern.compile("^(\\d{1,3})班$");
    private static final int MIN_CLASS_NAME = 1;
    private static final int MAX_CLASS_NAME = 30;

    private final StudentInfoRepository studentInfoRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final StudentAvatarStorageService studentAvatarStorageService;
    private final StudentProfileCompletionService studentProfileCompletionService;

    public StudentService(StudentInfoRepository studentInfoRepository,
                          UserRepository userRepository,
                          CurrentUserProvider currentUserProvider,
                          PasswordEncoder passwordEncoder,
                          StudentAvatarStorageService studentAvatarStorageService,
                          StudentProfileCompletionService studentProfileCompletionService) {
        this.studentInfoRepository = studentInfoRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.studentAvatarStorageService = studentAvatarStorageService;
        this.studentProfileCompletionService = studentProfileCompletionService;
    }

    @Transactional(readOnly = true)
    public StudentInfoResponse getCurrentInfo() {
        UserEntity user = getRequiredStudentUserEntity();
        StudentInfoEntity info = studentInfoRepository.findByUserId(user.getId()).orElse(null);
        return toResponse(user, info);
    }

    @Transactional
    public StudentInfoResponse updateCurrentInfo(UpdateStudentInfoRequest request) {
        UserEntity user = getRequiredStudentUserEntity();
        StudentInfoEntity info = studentInfoRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyInfo(user.getId()));

        info.setDisplayName(trimToNull(request.displayName()));
        info.setStudentNo(trimToNull(request.studentNo()));
        info.setGrade(normalizeGrade(request.grade()));
        info.setClassName(normalizeClassName(request.className()));
        info.setPhone(trimToNull(request.phone()));
        info.setBio(trimToNull(request.bio()));

        StudentInfoEntity saved = studentInfoRepository.save(info);
        return toResponse(user, saved);
    }

    @Transactional
    public StudentInfoResponse uploadCurrentAvatar(MultipartFile file) {
        UserEntity user = getRequiredStudentUserEntity();
        StudentInfoEntity info = studentInfoRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyInfo(user.getId()));
        StoredStudentAvatar storedAvatar = studentAvatarStorageService.store(file);
        String previousAvatarPath = info.getAvatarPath();

        try {
            info.setAvatarPath(storedAvatar.avatarPath());
            info.setAvatarUpdatedAt(Instant.now());
            StudentInfoEntity saved = studentInfoRepository.save(info);
            studentAvatarStorageService.deleteIfExists(previousAvatarPath);
            return toResponse(user, saved);
        } catch (RuntimeException exception) {
            studentAvatarStorageService.deleteIfExists(storedAvatar.avatarPath());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public StudentAvatarContent resolveCurrentAvatar() {
        UserEntity user = getRequiredStudentUserEntity();
        StudentInfoEntity info = studentInfoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "student avatar not found"));
        if (info.getAvatarPath() == null || info.getAvatarPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "student avatar not found");
        }
        return studentAvatarStorageService.resolveContent(info.getAvatarPath());
    }

    @Transactional
    public void changeCurrentPassword(ChangeStudentPasswordRequest request) {
        UserEntity user = getRequiredStudentUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "current password is incorrect");
        }

        String nextPassword = String.valueOf(request.newPassword()).trim();
        if (!nextPassword.equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "new password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(nextPassword));
        userRepository.save(user);
    }

    private StudentInfoEntity createEmptyInfo(Long userId) {
        StudentInfoEntity info = new StudentInfoEntity();
        info.setUserId(userId);
        return info;
    }

    private StudentInfoResponse toResponse(UserEntity user, StudentInfoEntity info) {
        String displayName = info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank()
                ? info.getDisplayName()
                : user.getUsername();
        boolean hasAvatar = info != null && info.getAvatarPath() != null && !info.getAvatarPath().isBlank();
        StudentProfileCompletion completion = studentProfileCompletionService.evaluate(info);

        return new StudentInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEmail(),
                displayName,
                info == null ? "" : orEmpty(info.getStudentNo()),
                info == null ? "" : orEmpty(normalizeGrade(info.getGrade())),
                info == null ? "" : orEmpty(normalizeClassName(info.getClassName())),
                info == null ? "" : orEmpty(info.getPhone()),
                info == null ? "" : orEmpty(info.getBio()),
                hasAvatar,
                info == null ? null : info.getAvatarUpdatedAt(),
                completion.completed(),
                completion.missingRequiredFields()
        );
    }

    private UserEntity getRequiredStudentUserEntity() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private String trimToNull(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeGrade(String value) {
        return StudentGrade.normalize(value);
    }

    private String normalizeClassName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        Matcher plainMatcher = CLASS_NAME_PATTERN.matcher(normalized);
        if (plainMatcher.matches()) {
            return normalizeClassNumber(plainMatcher.group(1));
        }

        Matcher legacyMatcher = LEGACY_CLASS_NAME_PATTERN.matcher(normalized);
        if (legacyMatcher.matches()) {
            return normalizeClassNumber(legacyMatcher.group(1));
        }

        return null;
    }

    private String normalizeClassNumber(String value) {
        int numericValue = Integer.parseInt(value);
        if (numericValue < MIN_CLASS_NAME || numericValue > MAX_CLASS_NAME) {
            return null;
        }
        return String.valueOf(numericValue);
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
