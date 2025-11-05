package com.example.lms.controller;

import com.example.lms.config.JwtAuthFilter;
import com.example.lms.config.JwtUtils;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // ✅ disables Spring Security filters
@ActiveProfiles("test")
public class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthenticationManager authenticationManager;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private JwtUtils jwtUtils;

  @MockitoBean
  private JwtAuthFilter jwtAuthFilter;

  @Test
  void register_returnsOk_whenSuccess() throws Exception {
    // Mock the behavior
    Mockito.when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.empty());

    Mockito.when(passwordEncoder.encode(anyString()))
        .thenReturn("encodedPassword");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setEmail("test@example.com");
    savedUser.setUsername("testuser");
    savedUser.setPassword("encodedPassword");
    savedUser.setRole(Role.STUDENT);

    Mockito.when(userRepository.save(any(User.class)))
        .thenReturn(savedUser);

    // Perform the request
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "testuser",
                  "email": "test@example.com",
                  "password": "secret123"
                }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("User registered successfully"))
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void login_returnsToken_whenCredentialsValid() throws Exception {
    User existingUser = new User();
    existingUser.setEmail("test@example.com");
    existingUser.setPassword("encodedPassword");
    existingUser.setRole(Role.STUDENT);

    Mockito.when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(existingUser));

    Mockito.when(jwtUtils.generateToken(anyString(), anyString()))
        .thenReturn("fake-jwt-token");

    Mockito.when(authenticationManager.authenticate(any(Authentication.class)))
        .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "secret123"));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "email": "test@example.com",
                  "password": "secret123"
                }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("fake-jwt-token"));
  }

  // ✅ Case 2: Missing email
  @Test
  void register_returnsBadRequest_whenEmailMissing() throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "user1",
                  "password": "secret123"
                }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Email is required"));
  }

  // ✅ Case 3: Missing password
  @Test
  void register_returnsBadRequest_whenPasswordMissing() throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "user1",
                  "email": "user1@example.com"
                }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Password is required"));
  }

  // ✅ Case 4: Duplicate email
  @Test
  void register_returnsBadRequest_whenEmailExists() throws Exception {
    User existing = new User();
    existing.setId(1L);
    existing.setEmail("exists@example.com");
    Mockito.when(userRepository.findByEmail("exists@example.com")).thenReturn(Optional.of(existing));

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "user1",
                  "email": "exists@example.com",
                  "password": "secret123"
                }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Email already exists"));
  }

  // ✅ Case 5: Unexpected exception
  @Test
  void register_returns500_whenUnexpectedError() throws Exception {
    Mockito.when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("DB error"));

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "user1",
                  "email": "fail@example.com",
                  "password": "secret123"
                }
            """))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("Registration failed"))
        .andExpect(jsonPath("$.detail").value("DB error"));
  }

  @Test
  void register_returnsInternalServerError_whenExceptionOccurs() throws Exception {
    // Mock repository to throw an exception
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encodedpass");
    when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "username": "test",
                  "email": "error@example.com",
                  "password": "pass123"
                }
            """))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value("Registration failed"))
        .andExpect(jsonPath("$.detail").value("DB error"));
  }

}
