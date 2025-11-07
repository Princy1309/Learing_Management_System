package com.example.lms.service;

import com.example.lms.dto.RegisterDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.repository.CourseRepository;
import com.example.lms.repository.UserRepository;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAllUsers_delegatesToRepository() {
        User u1 = TestDataFactory.makeUser(1L, "a", "a@x.com", com.example.lms.entity.Role.STUDENT);
        User u2 = TestDataFactory.makeUser(2L, "b", "b@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> res = adminService.findAllUsers();
        assertEquals(2, res.size());
        assertSame(u1, res.get(0));
        verify(userRepository).findAll();
    }

    @Test
    void updateUserRole_success_changesRoleAndSaves() {
        User user = TestDataFactory.makeUser(10L, "joe", "joe@x.com", com.example.lms.entity.Role.STUDENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = adminService.updateUserRole(10L, Role.ADMIN);
        assertEquals(Role.ADMIN, updated.getRole());
        verify(userRepository).findById(10L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.updateUserRole(99L, Role.ADMIN));
        assertTrue(ex.getMessage().toLowerCase().contains("user not found"));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_callsDeleteById() {
        // just verify delegation
        doNothing().when(userRepository).deleteById(15L);
        adminService.deleteUser(15L);
        verify(userRepository).deleteById(15L);
    }

    @Test
    void registerUser_delegatesToAuthService_andReturnsToken() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("u1");
        dto.setEmail("u1@x.com");
        dto.setPassword("secret");
        dto.setRole(Role.STUDENT);

        when(authService.register("u1", "u1@x.com", "secret", Role.STUDENT)).thenReturn("token-abc");

        String token = adminService.registerUser(dto);
        assertEquals("token-abc", token);
        verify(authService).register("u1", "u1@x.com", "secret", Role.STUDENT);
    }

    @Test
    void findPendingCourses_returnsRepoList() {
        Course c1 = TestDataFactory.makeCourse(1L, "C1", "d", null, false);
        when(courseRepository.findByApprovedFalseAndIsDeletedFalse()).thenReturn(List.of(c1));

        List<Course> pending = adminService.findPendingCourses();
        assertEquals(1, pending.size());
        assertSame(c1, pending.get(0));
        verify(courseRepository).findByApprovedFalseAndIsDeletedFalse();
    }

    @Test
    void approveCourse_success_setsApprovedAndSaves() {
        Course c = TestDataFactory.makeCourse(50L, "C", "d", null, false);
        when(courseRepository.findById(50L)).thenReturn(Optional.of(c));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        adminService.approveCourse(50L);

        // check the boolean getter that actually exists
        assertTrue(c.isApproved(), "Course should be approved (true)");
        verify(courseRepository).findById(50L);
        verify(courseRepository).save(c);
    }

    @Test
    void approveCourse_notFound_throws() {
        when(courseRepository.findById(777L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.approveCourse(777L));
        assertTrue(ex.getMessage().toLowerCase().contains("course not found"));
        verify(courseRepository).findById(777L);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void removeCourse_callsDeleteById() {
        doNothing().when(courseRepository).deleteById(123L);
        adminService.removeCourse(123L);
        verify(courseRepository).deleteById(123L);
    }
}
