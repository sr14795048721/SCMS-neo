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

class OneTimeAdminInitializerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AdminInitProperties properties;
    private OneTimeAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        properties = new AdminInitProperties();
        initializer = new OneTimeAdminInitializer(userRepository, passwordEncoder, properties);
    }

    @Test
    void runShouldCreateAdminWhenNoneExists() throws Exception {
        properties.setEnabled(true);
        properties.setUsername("root-admin");
        properties.setEmail("ADMIN@example.com");
        properties.setPassword("super-secret");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("root-admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("super-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("root-admin", saved.getUsername());
        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("encoded-secret", saved.getPasswordHash());
        assertEquals(UserRole.ADMIN, saved.getRole());
    }

    @Test
    void runShouldSkipWhenAdminAlreadyExists() throws Exception {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void runShouldRejectMissingUsername() {
        properties.setEnabled(true);
        properties.setEmail("admin@example.com");
        properties.setPassword("secret");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run(new DefaultApplicationArguments(new String[0]))
        );

        assertEquals(
                "SCMS_ADMIN_INIT_USERNAME is required when SCMS_ADMIN_INIT_ENABLED=true",
                exception.getMessage()
        );
    }

    @Test
    void runShouldRejectUsernameConflict() {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setEmail("admin@example.com");
        properties.setPassword("secret");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run(new DefaultApplicationArguments(new String[0]))
        );

        assertEquals("SCMS_ADMIN_INIT_USERNAME already exists", exception.getMessage());
    }
}
