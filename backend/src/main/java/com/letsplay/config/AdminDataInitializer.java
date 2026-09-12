package com.letsplay.config;

import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name:Admin User}")
    private String adminName;

    @Value("${app.admin.email:admin@letsplay.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Autowired
    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            String email = (adminEmail != null) ? adminEmail.trim().toLowerCase() : "admin@letsplay.com";
            if (!userRepository.existsByEmailIgnoreCase(email)) {
                User admin = User.builder()
                        .name(adminName != null ? adminName.trim() : "Admin User")
                        .email(email)
                        .password(passwordEncoder.encode(adminPassword != null ? adminPassword : "admin123"))
                        .role("ADMIN")
                        .build();
                userRepository.save(admin);
                log.info("Initialized default administrator account on setup: {}", email);
            } else {
                log.info("Administrator account already exists: {}", email);
            }
        } catch (Exception e) {
            log.warn("Could not initialize default administrator on startup: {}", e.getMessage());
        }
    }
}
