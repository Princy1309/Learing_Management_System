package com.example.lms.service;

import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CustomUserDetailsService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadByUsername_found_returnsUserDetails() {
        User u = TestDataFactory.makeUser(1L, "stu", "stu@x.com", com.example.lms.entity.Role.STUDENT);
        when(userRepository.findByEmail("stu@x.com")).thenReturn(Optional.of(u));
        var ud = service.loadUserByUsername("stu@x.com");
        assertNotNull(ud);
        assertTrue(ud.getUsername().equals("stu@x.com") || ud.getUsername().equals(u.getEmail()));
    }

    @Test
    void loadByUsername_notFound_throws() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.loadUserByUsername("no@x.com"));
    }
}
