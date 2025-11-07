package com.example.lms.repository;

import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
// disable Flyway / Liquibase and let JPA create-drop schema for tests
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("secret")
                .role(Role.STUDENT)
                .build();
        savedUser = userRepository.save(user);
        // flush is optional; repository.save will persist for tests with create-drop
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("test@example.com");
        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.get().getId());
    }

    @Test
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("none@example.com");
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsername_existingUsername_returnsUser() {
        Optional<User> result = userRepository.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals(savedUser.getEmail(), result.get().getEmail());
    }

    @Test
    void findByUsername_nonExistingUsername_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertTrue(userRepository.existsByEmail("test@example.com"));
    }

    @Test
    void existsByEmail_nonExistingEmail_returnsFalse() {
        assertFalse(userRepository.existsByEmail("fake@example.com"));
    }
}
