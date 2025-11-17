package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.demo.domain.User;
import com.example.demo.enums.Role;
import com.example.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.username:Administrator}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            return;
        }

        userRepo.findByEmail(adminEmail)
                .ifPresentOrElse(this::ensureAdminRole, this::createAdminUser);
    }

    private void ensureAdminRole(User user) {
        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            userRepo.save(user);
            log.info("Existing user {} was promoted to ADMIN role", user.getEmail());
        }
    }

    private void createAdminUser() {
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername(resolveUsername());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);
        log.info("Admin account seeded for email {}", adminEmail);
    }

    private String resolveUsername() {
        if (StringUtils.hasText(adminUsername)) {
            return adminUsername;
        }
        int atIndex = adminEmail.indexOf('@');
        return atIndex > 0 ? adminEmail.substring(0, atIndex) : "admin";
    }
}
