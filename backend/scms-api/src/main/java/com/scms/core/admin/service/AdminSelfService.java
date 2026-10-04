package com.scms.core.admin.service;

import com.scms.core.admin.dto.ChangeAdminPasswordRequest;
import com.scms.core.audit.service.AuditService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSelfService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminSelfService(UserRepository userRepository,
                            CurrentUserProvider currentUserProvider,
                            PasswordEncoder passwordEncoder,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void changeCurrentPassword(ChangeAdminPasswordRequest request) {
        UserEntity user = getRequiredAdminUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "current password is incorrect");
        }

        String nextPassword = String.valueOf(request.newPassword()).trim();
        if (!nextPassword.equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "new password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(nextPassword));
        userRepository.save(user);
        auditService.create(
                "ADMIN_PASSWORD_CHANGED",
                user.getId(),
                "USER",
                String.valueOf(user.getId()),
                "Administrator changed own password"
        );
    }

    private UserEntity getRequiredAdminUserEntity() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user not found"));
    }
}
