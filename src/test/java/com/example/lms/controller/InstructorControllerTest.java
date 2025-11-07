package com.example.lms.controller;

import com.example.lms.dto.CourseCreateDto;
import com.example.lms.dto.CourseUpdateDto;
import com.example.lms.dto.StudentProgressDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import com.example.lms.service.CourseService;
import com.example.lms.service.CourseServiceImpl;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InstructorControllerTest {

    @Mock
    private CourseService courseService;
    @Mock
    private UserRepository userRepository; // <-- use the actual repository interface
    @Mock
    private CourseServiceImpl courseServiceImpl;

    @InjectMocks
    private InstructorController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCoursePage_returnsViewName() {
        assertEquals("instructor/create-course", controller.createCoursePage());
    }

    @Test
    void manageCoursesPage_returnsViewName() {
        assertEquals("instructor/manage-courses", controller.manageCoursesPage());
    }

    @Test
    void enrolledStudentsPage_returnsViewName() {
        assertEquals("instructor/enrolled-students", controller.enrolledStudentsPage());
    }

    // --- createCourse: unauthorized when auth missing or not authenticated ---
    @Test
    void createCourse_unauthorized_whenAuthNull() {
        CourseCreateDto dto = new CourseCreateDto();
        ResponseEntity<?> resp = controller.createCourse(dto, null);
        assertEquals(401, resp.getStatusCode().value());
        assertEquals("Unauthorized", resp.getBody());
    }

    @Test
    void createCourse_unauthorized_whenNotAuthenticated() {
        CourseCreateDto dto = new CourseCreateDto();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<?> resp = controller.createCourse(dto, auth);
        assertEquals(401, resp.getStatusCode().value());
        assertEquals("Unauthorized", resp.getBody());
    }

    @Test
    void createCourse_instructorNotFound_returns400() {
        CourseCreateDto dto = new CourseCreateDto();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("no@x.com");

        // Important: stub the real userRepository (not null)
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.createCourse(dto, auth);
        assertEquals(400, resp.getStatusCode().value());
        assertEquals("Instructor not found", resp.getBody());
    }

    @Test
    void createCourse_success_callsServiceAndReturnsSaved() {
        CourseCreateDto dto = new CourseCreateDto();
        dto.setTitle("T");
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(10L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        Course saved = TestDataFactory.makeCourse(77L, "T", "d", inst, false);
        // more robust: use any() for dto matching if dto identity isn't same
        when(courseService.createCourseFromDto(any(CourseCreateDto.class), eq(inst))).thenReturn(saved);

        ResponseEntity<?> resp = controller.createCourse(dto, auth);
        assertEquals(200, resp.getStatusCode().value());
        assertSame(saved, resp.getBody());
        verify(courseService).createCourseFromDto(any(CourseCreateDto.class), eq(inst));
    }

    @Test
    void createCourse_serviceThrows_returns500WithMessage() {
        CourseCreateDto dto = new CourseCreateDto();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("inst@x.com");

        // Make userRepository throw so controller's try{} will throw and go to catch{}
        doThrow(new RuntimeException("boom")).when(userRepository).findByEmail("inst@x.com");

        ResponseEntity<?> resp = controller.createCourse(dto, auth);

        assertEquals(500, resp.getStatusCode().value());
        String body = resp.getBody().toString().toLowerCase();
        assertTrue(body.contains("failed to create course"));
        assertTrue(body.contains("boom"));
    }

    // --- getMyCourses (uses courseServiceImpl.getCoursesByInstructor) ---
    @Test
    void getMyCourses_returnsCoursesForInstructor() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(12L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        Course c = TestDataFactory.makeCourse(88L, "X", "d", inst, false);
        when(courseServiceImpl.getCoursesByInstructor(inst)).thenReturn(List.of(c));

        ResponseEntity<?> resp = controller.getMyCourses(auth);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody() instanceof List);
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());
    }

    // --- getCourseById with various branches ---
    @Test
    void getCourseById_notFound_returns404() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(13L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        when(courseServiceImpl.getCourseById(999L)).thenReturn(null);

        ResponseEntity<?> resp = controller.getCourseById(999L, auth);
        assertEquals(404, resp.getStatusCode().value());
        assertEquals("Course not found", resp.getBody());
    }

    @Test
    void getCourseById_forbiddenWhenNotOwner() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(14L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        User other = TestDataFactory.makeUser(15L, "other", "other@x.com", com.example.lms.entity.Role.INSTRUCTOR);

        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));
        Course course = TestDataFactory.makeCourse(999L, "C", "d", other, false);
        when(courseServiceImpl.getCourseById(999L)).thenReturn(course);

        ResponseEntity<?> resp = controller.getCourseById(999L, auth);
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("Forbidden", resp.getBody());
    }

    @Test
    void getCourseById_success_returnsCourse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(16L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));
        Course course = TestDataFactory.makeCourse(999L, "C", "d", inst, false);
        when(courseServiceImpl.getCourseById(999L)).thenReturn(course);

        ResponseEntity<?> resp = controller.getCourseById(999L, auth);
        assertEquals(200, resp.getStatusCode().value());
        assertSame(course, resp.getBody());
    }

    // --- updateCourse success and exception ---
    @Test
    void updateCourse_success_returnsUpdated() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(17L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        CourseUpdateDto dto = new CourseUpdateDto();
        Course updated = TestDataFactory.makeCourse(123L, "updated", "d", inst, false);
        when(courseServiceImpl.updateCourse(123L, dto, inst)).thenReturn(updated);

        ResponseEntity<?> resp = controller.updateCourse(123L, dto, auth);
        assertEquals(200, resp.getStatusCode().value());
        assertSame(updated, resp.getBody());
    }

    @Test
    void updateCourse_serviceThrows_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(18L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        CourseUpdateDto dto = new CourseUpdateDto();
        when(courseServiceImpl.updateCourse(123L, dto, inst)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = controller.updateCourse(123L, dto, auth);
        assertEquals(500, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().toLowerCase().contains("boom"));
    }

    // --- deleteCourse success and exception ---
    @Test
    void deleteCourse_success_returnsMessage() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(19L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        doNothing().when(courseServiceImpl).softDeleteCourse(55L, inst);

        ResponseEntity<?> resp = controller.deleteCourse(55L, auth);
        assertEquals(200, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("deleted"));
    }

    @Test
    void deleteCourse_serviceThrows_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(20L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        doThrow(new RuntimeException("boom")).when(courseServiceImpl).softDeleteCourse(55L, inst);

        ResponseEntity<?> resp = controller.deleteCourse(55L, auth);
        assertEquals(500, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().toLowerCase().contains("boom"));
    }

    // --- approveCourse success and exception ---
    @Test
    void approveCourse_success_returnsMessage() {
        doNothing().when(courseServiceImpl).approveCourse(77L);

        ResponseEntity<?> resp = controller.approveCourse(77L);
        assertEquals(200, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("message").toString().toLowerCase().contains("approved"));
    }

    @Test
    void approveCourse_serviceThrows_returns500() {
        doThrow(new RuntimeException("boom")).when(courseServiceImpl).approveCourse(77L);

        ResponseEntity<?> resp = controller.approveCourse(77L);
        assertEquals(500, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().toLowerCase().contains("boom"));
    }

    // --- getEnrolledStudents success and exception ---
    @Test
    void getEnrolledStudents_success_returnsList() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(21L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        StudentProgressDto dto = new StudentProgressDto();
        when(courseServiceImpl.getEnrolledStudentsWithProgress(99L, inst)).thenReturn(List.of(dto));

        ResponseEntity<?> resp = controller.getEnrolledStudents(99L, auth);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody() instanceof List);
    }

    @Test
    void getEnrolledStudents_serviceThrows_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("inst@x.com");

        User inst = TestDataFactory.makeUser(22L, "ins", "inst@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        when(userRepository.findByEmail("inst@x.com")).thenReturn(Optional.of(inst));

        when(courseServiceImpl.getEnrolledStudentsWithProgress(99L, inst)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = controller.getEnrolledStudents(99L, auth);
        assertEquals(500, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) body;
        assertTrue(m.get("error").toString().toLowerCase().contains("boom"));
    }

}