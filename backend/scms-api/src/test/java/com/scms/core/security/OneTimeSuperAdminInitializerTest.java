package com.scms.core.security;

import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OneTimeSuperAdminInitializerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SuperAdminInitProperties properties;
    private OneTimeSuperAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        properties = new SuperAdminInitProperties();
        initializer = new OneTimeSuperAdminInitializer(userRepository, passwordEncoder, properties);
    }

    @Test
    void runShouldCreateSuperAdminWhenNoneExists() throws Exception {
        properties.setEnabled(true);
        properties.setUsername("root-super-admin");
        properties.setEmail("SUPERADMIN@example.com");
        properties.setPassword("super-secret");

        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("root-super-admin")).thenReturn(false);
        when(userRepository.existsByEmail("superadmin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("super-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("root-super-admin", saved.getUsername());
        assertEquals("superadmin@example.com", saved.getEmail());
        assertEquals("encoded-secret", saved.getPasswordHash());
        assertEquals(UserRole.SUPER_ADMIN, saved.getRole());
    }

    @Test
    void runShouldSkipWhenSuperAdminAlreadyExists() throws Exception {
        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(true);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void runShouldRejectMissingUsername() {
        properties.setEnabled(true);
        properties.setEmail("superadmin@example.com");
        properties.setPassword("secret");

        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run(new DefaultApplicationArguments(new String[0]))
        );

        assertEquals(
                "SCMS_SUPER_ADMIN_INIT_USERNAME is required when SCMS_SUPER_ADMIN_INIT_ENABLED=true",
                exception.getMessage()
        );
    }

    @Test
    void runShouldRejectUsernameConflict() {
        properties.setEnabled(true);
        properties.setUsername("super-admin");
        properties.setEmail("superadmin@example.com");
        properties.setPassword("secret");

        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("super-admin")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run(new DefaultApplicationArguments(new String[0]))
        );

        assertEquals("SCMS_SUPER_ADMIN_INIT_USERNAME already exists", exception.getMessage());
    }
}
