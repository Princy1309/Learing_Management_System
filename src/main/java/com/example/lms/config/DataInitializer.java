package com.example.lms.config;

import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@lms.com").isEmpty()) {
                User admin = User.builder()
                        .username("Admin")
                        .email("admin@lms.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Default admin created: admin@lms.com / admin123");
            }
        };
    }
}
