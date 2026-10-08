package com.freelancer.config;

import com.freelancer.model.User;
import com.freelancer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountSeeder.class);
    private static final String ADMIN_EMAIL = "rajshri@admin.com";
    private static final String ADMIN_PASSWORD = "rajshri123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(User::new);
            admin.setFullName("FreelanceHub Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(User.Role.ADMIN);
            userRepository.saveAndFlush(admin);
        } catch (DataAccessException exception) {
            logger.error("Could not create or update the admin account; application startup will continue.", exception);
        }
    }
}
