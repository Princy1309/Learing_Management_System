package com.example.lms.controller;

import com.example.lms.dto.CourseDto;
import com.example.lms.dto.MyCourseDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.User;
import com.example.lms.repository.LessonProgressRepository;
import com.example.lms.repository.UserRepository;
import com.example.lms.service.StudentService;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StudentControllerTest {

    @Mock
    private StudentService studentService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private StudentController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------- getApprovedCourses ----------
    @Test
    void getApprovedCourses_success_returnsList() {
        Course c1 = TestDataFactory.makeCourse(1L, "A", "a", null, true);
        when(studentService.getApprovedCourses()).thenReturn(List.of(c1));

        ResponseEntity<List<Course>> resp = controller.getApprovedCourses();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getApprovedCourses_exception_returns500AndEmpty() {
        when(studentService.getApprovedCourses()).thenThrow(new RuntimeException("boom"));
        ResponseEntity<List<Course>> resp = controller.getApprovedCourses();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(0, resp.getBody().size());
    }

    // ---------- enrollInCourse ----------
    @Test
    void enrollInCourse_userDetailsNull_returns401() {
        ResponseEntity<?> resp = controller.enrollInCourse(5L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Unauthorized", resp.getBody());
    }

    @Test
    void enrollInCourse_success_returnsOkMessage() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        doNothing().when(studentService).enrollStudent(7L, "s@x.com");

        ResponseEntity<?> resp = controller.enrollInCourse(7L, ud);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Enrolled successfully", resp.getBody());
        verify(studentService).enrollStudent(7L, "s@x.com");
    }

    @Test
    void enrollInCourse_whenIllegalState_returnsBadRequestWithMessage() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        doThrow(new IllegalStateException("already")).when(studentService).enrollStudent(8L, "s@x.com");

        ResponseEntity<?> resp = controller.enrollInCourse(8L, ud);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("already", resp.getBody());
    }

    @Test
    void enrollInCourse_onUnexpectedException_returns500() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        doThrow(new RuntimeException("boom")).when(studentService).enrollStudent(9L, "s@x.com");

        ResponseEntity<?> resp = controller.enrollInCourse(9L, ud);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Enrollment failed", resp.getBody());
    }

    // ---------- getMyEnrollments ----------
    @Test
    void getMyEnrollments_userDetailsNull_returns401() {
        ResponseEntity<List<Enrollment>> resp = controller.getMyEnrollments(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getMyEnrollments_success_returnsEnrollments() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        Enrollment e = TestDataFactory.makeEnrollment(
                11L,
                TestDataFactory.makeUser(2L, "stu", "s@x.com", com.example.lms.entity.Role.STUDENT),
                TestDataFactory.makeCourse(12L, "C", "d", null, true));
        when(studentService.getEnrollments("s@x.com")).thenReturn(List.of(e));

        ResponseEntity<List<Enrollment>> resp = controller.getMyEnrollments(ud);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getMyEnrollments_exception_returns500AndEmpty() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        when(studentService.getEnrollments("s@x.com")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<List<Enrollment>> resp = controller.getMyEnrollments(ud);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(0, resp.getBody().size());
    }

    // ---------- getCourseForStudent ----------
    @Test
    void getCourseForStudent_courseNotFound_returns404() {
        Principal p = () -> "s@x.com";
        when(studentService.getCourseById(100L)).thenReturn(Optional.empty());
        ResponseEntity<?> resp = controller.getCourseForStudent(100L, p);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        Object responseBody = resp.getBody();
        assertTrue(responseBody instanceof Map, "Response body should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseBody;

        assertEquals("Course not found", body.get("error"));
    }

    @Test
    void getCourseForStudent_principalNull_returns403() {
        Course c = TestDataFactory.makeCourse(200L, "C", "d", null, true);
        when(studentService.getCourseById(200L)).thenReturn(Optional.of(c));

        ResponseEntity<?> resp = controller.getCourseForStudent(200L, null);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("You must be logged in to view this course", resp.getBody());
    }

    @Test
    void getCourseForStudent_courseNotFound() {
        Principal p = () -> "s@x.com";

        // make sure the service returns empty for this exact id
        doReturn(Optional.empty()).when(studentService).getCourseById(100L);

        ResponseEntity<?> resp = controller.getCourseForStudent(100L, p);

        // non-deprecated status check
        assertEquals(404, resp.getStatusCode().value());

        // safe, warning-free cast
        Object responseBody = resp.getBody();
        assertTrue(responseBody instanceof Map, "Body should be a Map");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseBody;

        // assert the error message is the expected one
        assertTrue(body.get("error").toString().toLowerCase().contains("course not found"));

        // verification: ensure we stubbed the right call and userRepo was not queried
        verify(studentService).getCourseById(100L);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getCourseForStudent_notEnrolled_returns403() {
        Principal p = () -> "s@x.com";
        Course c = TestDataFactory.makeCourse(202L, "C", "d", null, true);
        when(studentService.getCourseById(202L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(
                TestDataFactory.makeUser(3L, "s", "s@x.com", com.example.lms.entity.Role.STUDENT)));
        when(studentService.isStudentEnrolledInCourse("s@x.com", c)).thenReturn(false);

        ResponseEntity<?> resp = controller.getCourseForStudent(202L, p);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("You are not enrolled in this course", resp.getBody());
    }

    @Test
    void getCourseForStudent_success_returnsCourseDto() {
        Principal p = () -> "s@x.com";
        Course c = TestDataFactory.makeCourse(210L, "C", "d", null, true);
        User student = TestDataFactory.makeUser(4L, "s", "s@x.com", com.example.lms.entity.Role.STUDENT);

        when(studentService.getCourseById(210L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(student));
        when(studentService.isStudentEnrolledInCourse("s@x.com", c)).thenReturn(true);
        when(lessonProgressRepository.findByStudentAndLessonIn(student, c.getLessons())).thenReturn(List.of());

        ResponseEntity<?> resp = controller.getCourseForStudent(210L, p);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof CourseDto);
        CourseDto dto = (CourseDto) resp.getBody();
        assertEquals("C", dto.getTitle());
    }

    // ---------- getMyCourses ----------
    @Test
    void getMyCourses_userDetailsNull_returns401() {
        ResponseEntity<?> resp = controller.getMyCourses(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Unauthorized", resp.getBody());
    }

    @Test
    void getMyCourses_success_returnsDtos() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        MyCourseDto dto = new MyCourseDto();
        when(studentService.getMyCoursesWithProgress("s@x.com")).thenReturn(List.of(dto));

        ResponseEntity<?> resp = controller.getMyCourses(ud);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertInstanceOf(List.class, resp.getBody());
        assertEquals(1, ((List<?>) resp.getBody()).size());
    }

    @Test
    void getMyCourses_exception_returns500() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("s@x.com");
        when(studentService.getMyCoursesWithProgress("s@x.com")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = controller.getMyCourses(ud);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Something went wrong", resp.getBody());
    }

    // ---------- markLessonAsComplete ----------
    @Test
    void markLessonAsComplete_userDetailsNull_returns401() {
        ResponseEntity<?> r = controller.markLessonAsComplete(5L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals("Unauthorized: You must be logged in.", r.getBody());
    }

    @Test
    void getCourseForStudent_unexpectedException_returns500() {
        Principal p = () -> "s@x.com";

        // Force the service to throw an unexpected runtime exception
        doThrow(new RuntimeException("boom")).when(studentService).getCourseById(500L);

        ResponseEntity<?> resp = controller.getCourseForStudent(500L, p);

        // Assert status 500 (server error)
        assertEquals(500, resp.getStatusCode().value());

        // Safely examine response body (should be Map<String,Object> containing the
        // unexpected error message)
        Object responseBody = resp.getBody();
        assertTrue(responseBody instanceof Map, "Response body should be a Map");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseBody;

        assertTrue(body.get("error").toString().toLowerCase()
                .contains("unexpected error"), "Expected unexpected error message in body");

        // Verify the service was invoked and userRepository was not called (since we
        // failed early)
        verify(studentService).getCourseById(500L);
        verify(userRepository, never()).findByEmail(anyString());
    }
}
