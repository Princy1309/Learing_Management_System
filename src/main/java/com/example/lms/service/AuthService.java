package com.example.lms.service;

import com.example.lms.config.JwtService;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.exception.DuplicateEmailException;
import com.example.lms.exception.InvalidCredentialsException;
import com.example.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(String username, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.save(user);

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail(), user.getRole().name());
    }
}
