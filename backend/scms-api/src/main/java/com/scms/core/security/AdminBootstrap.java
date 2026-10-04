package com.scms.core.security;

import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "scms.bootstrap",
        name = "default-users",
        havingValue = "true",
        matchIfMissing = true
)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", "admin@scms.local", "admin123", UserRole.ADMIN);
        createIfMissing("manager", "manager@scms.local", "manager123", UserRole.CLUB_MANAGER);
        createIfMissing("student", "student@scms.local", "student123", UserRole.STUDENT);
    }

    private void createIfMissing(String username, String email, String password, UserRole role) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Default account created: {} ({})", username, role);
    }
}
