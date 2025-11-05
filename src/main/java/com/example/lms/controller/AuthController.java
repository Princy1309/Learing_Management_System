package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import com.example.lms.config.JwtUtils;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authManager, UserRepository userRepo, PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.authManager = authManager;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto dto) {
        try {
            // basic validation
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }

            if (userRepo.findByEmail(dto.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            // user.setRole(dto.getRole() != null ? dto.getRole() : Role.STUDENT);
            user.setRole(Role.STUDENT);

            User saved = userRepo.save(user);

            return ResponseEntity.ok(Map.of("message", "User registered successfully", "id", saved.getId()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed", "detail", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name());
    }
}
