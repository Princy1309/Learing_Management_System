package com.example.lms.controller;

import com.example.lms.dto.RegisterDto;
import com.example.lms.dto.RoleUpdateDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import com.example.lms.entity.Role;
import com.example.lms.service.AdminService;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private AdminService adminService;
    @InjectMocks
    private AdminController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_delegatesAndReturnsList() {
        User u1 = TestDataFactory.makeUser(1L, "a", "a@x.com", com.example.lms.entity.Role.STUDENT);
        User u2 = TestDataFactory.makeUser(2L, "b", "b@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(adminService.findAllUsers()).thenReturn(List.of(u1, u2));

        List<User> res = controller.getAllUsers();
        assertEquals(2, res.size());
        assertSame(u1, res.get(0));
        verify(adminService).findAllUsers();
    }

    @Test
    void registerUser_success_returnsOkMessage() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("u");
        dto.setEmail("u@x.com");
        dto.setPassword("pw");
        dto.setRole(Role.STUDENT);

        // success path - service returns token, but controller ignores it and returns
        // OK map
        when(adminService.registerUser(dto)).thenReturn("some-token");

        ResponseEntity<?> resp = controller.registerUser(dto);
        assertEquals(200, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("user created"));
        verify(adminService).registerUser(dto);
    }

    @Test
    void registerUser_serviceThrows_returnsBadRequestWithError() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("u");
        dto.setEmail("u@x.com");
        dto.setPassword("pw");
        dto.setRole(Role.STUDENT);

        when(adminService.registerUser(dto)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = controller.registerUser(dto);
        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("error").toString().toLowerCase().contains("boom"));
        verify(adminService).registerUser(dto);
    }

    @Test
    void updateUserRole_delegatesAndReturnsMessage() {
        RoleUpdateDto dto = new RoleUpdateDto();
        dto.setRole(Role.ADMIN);

        // mock the return value since updateUserRole returns a User
        User mockUser = new User();
        when(adminService.updateUserRole(5L, Role.ADMIN)).thenReturn(mockUser);

        ResponseEntity<?> resp = controller.updateUserRole(5L, dto);
        assertEquals(200, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("role updated"));
        verify(adminService).updateUserRole(5L, Role.ADMIN);
    }

    @Test
    void deleteUser_delegatesAndReturnsMessage() {
        doNothing().when(adminService).deleteUser(7L);

        ResponseEntity<?> resp = controller.deleteUser(7L);
        assertEquals(200, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("user deleted"));
        verify(adminService).deleteUser(7L);
    }

    @Test
    void getPendingCourses_delegatesAndReturnsList() {
        Course c = TestDataFactory.makeCourse(10L, "Pending", "d", null, false);
        when(adminService.findPendingCourses()).thenReturn(List.of(c));

        List<Course> res = controller.getPendingCourses();
        assertEquals(1, res.size());
        assertSame(c, res.get(0));
        verify(adminService).findPendingCourses();
    }

    @Test
    void approveCourse_delegatesAndReturnsMessage() {
        doNothing().when(adminService).approveCourse(33L);

        ResponseEntity<?> resp = controller.approveCourse(33L);
        assertEquals(200, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("course approved"));
        verify(adminService).approveCourse(33L);
    }

    @Test
    void removeCourse_delegatesAndReturnsMessage() {
        doNothing().when(adminService).removeCourse(44L);

        ResponseEntity<?> resp = controller.removeCourse(44L);
        assertEquals(200, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("course removed"));
        verify(adminService).removeCourse(44L);
    }
}
