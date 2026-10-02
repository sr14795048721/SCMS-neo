package com.scms.core.manager;

import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.dto.ChangeManagerPasswordRequest;
import com.scms.core.manager.dto.ManagerInfoResponse;
import com.scms.core.manager.dto.UpdateManagerInfoRequest;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.manager.service.ManagerAvatarStorageService;
import com.scms.core.manager.service.ManagerService;
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
import static org.mockito.Mockito.when;

class ManagerServiceTest {

    private ManagerInfoRepository managerInfoRepository;
    private UserRepository userRepository;
    private CurrentUserProvider currentUserProvider;
    private PasswordEncoder passwordEncoder;
    private ManagerAvatarStorageService managerAvatarStorageService;
    private ManagerService managerService;
    private UserEntity managerUser;

    @BeforeEach
    void setUp() {
        managerInfoRepository = mock(ManagerInfoRepository.class);
        userRepository = mock(UserRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        managerAvatarStorageService = mock(ManagerAvatarStorageService.class);
        managerService = new ManagerService(
                managerInfoRepository,
                userRepository,
                currentUserProvider,
                passwordEncoder,
                managerAvatarStorageService
        );

        managerUser = new UserEntity();
        managerUser.setUsername("manager");
        managerUser.setEmail("manager@scms.local");
        managerUser.setPasswordHash("encoded-old");
        managerUser.setRole(UserRole.CLUB_MANAGER);
        managerUser.setEnabled(true);

        setEntityId(managerUser, 7L);
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(7L, "manager", UserRole.CLUB_MANAGER));
        when(userRepository.findById(7L)).thenReturn(Optional.of(managerUser));
    }

    @Test
    void getCurrentInfoShouldFallbackToUserWhenManagerInfoMissing() {
        when(managerInfoRepository.findByUserId(7L)).thenReturn(Optional.empty());

        ManagerInfoResponse response = managerService.getCurrentInfo();

        assertEquals("manager", response.displayName());
        assertEquals("", response.managerNo());
        assertEquals("", response.phone());
        assertEquals("", response.bio());
    }

    @Test
    void updateCurrentInfoShouldCreateManagerInfoWhenMissing() {
        when(managerInfoRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(managerInfoRepository.save(any(ManagerInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManagerInfoResponse response = managerService.updateCurrentInfo(
                new UpdateManagerInfoRequest("张老师", "T2026001", "13800000000", "教师简介")
        );

        assertEquals("张老师", response.displayName());
        assertEquals("T2026001", response.managerNo());
        assertEquals("13800000000", response.phone());
        assertEquals("教师简介", response.bio());
    }

    @Test
    void changeCurrentPasswordShouldRejectIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong-old", "encoded-old")).thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> managerService.changeCurrentPassword(
                        new ChangeManagerPasswordRequest("wrong-old", "new-password", "new-password")
                )
        );
    }

    @Test
    void changeCurrentPasswordShouldUpdateHashWhenRequestIsValid() {
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        managerService.changeCurrentPassword(
                new ChangeManagerPasswordRequest("old-password", "new-password", "new-password")
        );

        assertEquals("encoded-new", managerUser.getPasswordHash());
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
