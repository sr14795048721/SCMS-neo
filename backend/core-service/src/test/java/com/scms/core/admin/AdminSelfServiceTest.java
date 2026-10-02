package com.scms.core.admin;

import com.scms.core.admin.dto.ChangeAdminPasswordRequest;
import com.scms.core.admin.service.AdminSelfService;
import com.scms.core.audit.service.AuditService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSelfServiceTest {

    private UserRepository userRepository;
    private CurrentUserProvider currentUserProvider;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private AdminSelfService adminSelfService;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        adminSelfService = new AdminSelfService(userRepository, currentUserProvider, passwordEncoder, auditService);

        adminUser = new UserEntity();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@scms.local");
        adminUser.setPasswordHash("encoded-old");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        setEntityId(adminUser, 1L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
    }

    @Test
    void changeCurrentPasswordShouldRejectIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong-old", "encoded-old")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminSelfService.changeCurrentPassword(
                        new ChangeAdminPasswordRequest("wrong-old", "new-password", "new-password")
                )
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void changeCurrentPasswordShouldRejectNonAdminUser() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "student", UserRole.STUDENT));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminSelfService.changeCurrentPassword(
                        new ChangeAdminPasswordRequest("old-password", "new-password", "new-password")
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void changeCurrentPasswordShouldUpdateHashWhenRequestIsValid() {
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminSelfService.changeCurrentPassword(
                new ChangeAdminPasswordRequest("old-password", "new-password", "new-password")
        );

        assertEquals("encoded-new", adminUser.getPasswordHash());
        verify(auditService).create(
                "ADMIN_PASSWORD_CHANGED",
                1L,
                "USER",
                "1",
                "Administrator changed own password"
        );
    }

    @Test
    void changeCurrentPasswordShouldAllowSuperAdmin() {
        adminUser.setRole(UserRole.SUPER_ADMIN);
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "super-admin", UserRole.SUPER_ADMIN));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminSelfService.changeCurrentPassword(
                new ChangeAdminPasswordRequest("old-password", "new-password", "new-password")
        );

        assertEquals("encoded-new", adminUser.getPasswordHash());
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
