package com.scms.core.manager.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.dto.ChangeManagerPasswordRequest;
import com.scms.core.manager.dto.ManagerInfoResponse;
import com.scms.core.manager.dto.UpdateManagerInfoRequest;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
public class ManagerService {

    private final ManagerInfoRepository managerInfoRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final ManagerAvatarStorageService managerAvatarStorageService;

    public ManagerService(ManagerInfoRepository managerInfoRepository,
                          UserRepository userRepository,
                          CurrentUserProvider currentUserProvider,
                          PasswordEncoder passwordEncoder,
                          ManagerAvatarStorageService managerAvatarStorageService) {
        this.managerInfoRepository = managerInfoRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.managerAvatarStorageService = managerAvatarStorageService;
    }

    @Transactional(readOnly = true)
    public ManagerInfoResponse getCurrentInfo() {
        UserEntity user = getRequiredManagerUserEntity();
        ManagerInfoEntity info = managerInfoRepository.findByUserId(user.getId()).orElse(null);
        return toResponse(user, info);
    }

    @Transactional
    public ManagerInfoResponse updateCurrentInfo(UpdateManagerInfoRequest request) {
        UserEntity user = getRequiredManagerUserEntity();
        ManagerInfoEntity info = managerInfoRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyInfo(user.getId()));

        info.setDisplayName(trimToNull(request.displayName()));
        info.setManagerNo(trimToNull(request.managerNo()));
        info.setPhone(trimToNull(request.phone()));
        info.setBio(trimToNull(request.bio()));

        ManagerInfoEntity saved = managerInfoRepository.save(info);
        return toResponse(user, saved);
    }

    @Transactional
    public ManagerInfoResponse uploadCurrentAvatar(MultipartFile file) {
        UserEntity user = getRequiredManagerUserEntity();
        ManagerInfoEntity info = managerInfoRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyInfo(user.getId()));
        StoredManagerAvatar storedAvatar = managerAvatarStorageService.store(file);
        String previousAvatarPath = info.getAvatarPath();

        try {
            info.setAvatarPath(storedAvatar.avatarPath());
            info.setAvatarUpdatedAt(Instant.now());
            ManagerInfoEntity saved = managerInfoRepository.save(info);
            managerAvatarStorageService.deleteIfExists(previousAvatarPath);
            return toResponse(user, saved);
        } catch (RuntimeException exception) {
            managerAvatarStorageService.deleteIfExists(storedAvatar.avatarPath());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ManagerAvatarContent resolveCurrentAvatar() {
        UserEntity user = getRequiredManagerUserEntity();
        ManagerInfoEntity info = managerInfoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "manager avatar not found"));
        if (info.getAvatarPath() == null || info.getAvatarPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "manager avatar not found");
        }
        return managerAvatarStorageService.resolveContent(info.getAvatarPath());
    }

    @Transactional
    public void changeCurrentPassword(ChangeManagerPasswordRequest request) {
        UserEntity user = getRequiredManagerUserEntity();
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

    private ManagerInfoEntity createEmptyInfo(Long userId) {
        ManagerInfoEntity info = new ManagerInfoEntity();
        info.setUserId(userId);
        return info;
    }

    private ManagerInfoResponse toResponse(UserEntity user, ManagerInfoEntity info) {
        String displayName = info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank()
                ? info.getDisplayName()
                : user.getUsername();
        boolean hasAvatar = info != null && info.getAvatarPath() != null && !info.getAvatarPath().isBlank();

        return new ManagerInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEmail(),
                displayName,
                info == null ? "" : orEmpty(info.getManagerNo()),
                info == null ? "" : orEmpty(info.getPhone()),
                info == null ? "" : orEmpty(info.getBio()),
                hasAvatar,
                info == null ? null : info.getAvatarUpdatedAt()
        );
    }

    private UserEntity getRequiredManagerUserEntity() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private String trimToNull(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
