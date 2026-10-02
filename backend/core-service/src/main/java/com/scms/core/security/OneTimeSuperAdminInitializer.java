package com.scms.core.security;

import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "scms.super-admin-init", name = "enabled", havingValue = "true")
public class OneTimeSuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OneTimeSuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminInitProperties properties;

    public OneTimeSuperAdminInitializer(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        SuperAdminInitProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("Skipping one-time super administrator initialization because a super administrator already exists");
            return;
        }

        String username = sanitize(properties.getUsername());
        String email = normalizeEmail(properties.getEmail());
        String password = properties.getPassword() == null ? "" : properties.getPassword().trim();

        validateInputs(username, email, password);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("SCMS_SUPER_ADMIN_INIT_USERNAME already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("SCMS_SUPER_ADMIN_INIT_EMAIL already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.SUPER_ADMIN);
        user.setEnabled(true);
        userRepository.save(user);

        log.info("One-time super administrator created: {}", username);
    }

    private void validateInputs(String username, String email, String password) {
        if (username.isBlank()) {
            throw new IllegalStateException("SCMS_SUPER_ADMIN_INIT_USERNAME is required when SCMS_SUPER_ADMIN_INIT_ENABLED=true");
        }
        if (email.isBlank()) {
            throw new IllegalStateException("SCMS_SUPER_ADMIN_INIT_EMAIL is required when SCMS_SUPER_ADMIN_INIT_ENABLED=true");
        }
        if (password.isBlank()) {
            throw new IllegalStateException("SCMS_SUPER_ADMIN_INIT_PASSWORD is required when SCMS_SUPER_ADMIN_INIT_ENABLED=true");
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return sanitize(value).toLowerCase(Locale.ROOT);
    }
}
