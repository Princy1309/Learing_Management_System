package com.example.lms.service;

import com.example.lms.config.JwtService;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.exception.DuplicateEmailException;
import com.example.lms.exception.InvalidCredentialsException;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    // --- Register Tests ---

    @Test
    void register_success_savesUserAndReturnsToken() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(jwtService.generateToken("a@b.com", "STUDENT")).thenReturn("jwt123");

        String token = authService.register("Alice", "a@b.com", "pass", Role.STUDENT);

        assertEquals("jwt123", token);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Alice", saved.getUsername());
        assertEquals("a@b.com", saved.getEmail());
        assertEquals("encodedPass", saved.getPassword());
        assertEquals(Role.STUDENT, saved.getRole());
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        when(userRepository.existsByEmail("x@y.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> authService.register("User", "x@y.com", "pw", Role.INSTRUCTOR));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // --- Login Tests ---

    @Test
    void login_success_returnsToken() {
        User user = User.builder()
                .email("a@b.com")
                .password("encodedPass")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "encodedPass")).thenReturn(true);
        when(jwtService.generateToken("a@b.com", "ADMIN")).thenReturn("token123");

        String token = authService.login("a@b.com", "raw");

        assertEquals("token123", token);
    }

    @Test
    void login_invalidEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login("no@x.com", "pw"));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        User user = User.builder()
                .email("a@b.com")
                .password("encoded")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login("a@b.com", "wrong"));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
    }
}
