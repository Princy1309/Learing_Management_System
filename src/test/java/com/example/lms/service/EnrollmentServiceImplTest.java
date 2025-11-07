package com.example.lms.service;

import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.User;
import com.example.lms.exception.DuplicateEnrollmentException;
import com.example.lms.repository.EnrollmentRepository;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void enrollStudent_success_savesAndReturnsEnrollment() {

        User student = TestDataFactory.makeUser(2L, "stu", "stu@x.com", com.example.lms.entity.Role.STUDENT);
        Course course = TestDataFactory.makeCourse(5L, "ct", "desc", null, false);

        when(enrollmentRepository.findByStudentAndCourse(student, course)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);

            try {
                e.setId(99L);
            } catch (Throwable ignored) {
            }
            return e;
        });

        // act
        Enrollment result = service.enrollStudent(student, course);

        // assert
        assertNotNull(result);
        assertEquals(student, result.getStudent());
        assertEquals(course, result.getCourse());
        verify(enrollmentRepository, times(1)).findByStudentAndCourse(student, course);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    void enrollStudent_whenAlreadyEnrolled_throwsDuplicateEnrollmentException() {
        // arrange
        User student = TestDataFactory.makeUser(2L, "stu", "stu@x.com", com.example.lms.entity.Role.STUDENT);
        Course course = TestDataFactory.makeCourse(5L, "ct", "desc", null, false);
        Enrollment existing = TestDataFactory.makeEnrollment(11L, student, course);

        when(enrollmentRepository.findByStudentAndCourse(student, course)).thenReturn(Optional.of(existing));

        // act / assert
        assertThrows(DuplicateEnrollmentException.class, () -> service.enrollStudent(student, course));
        verify(enrollmentRepository, times(1)).findByStudentAndCourse(student, course);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void getEnrollmentsByStudent_returnsList() {
        // arrange
        User student = TestDataFactory.makeUser(3L, "alice", "alice@x.com", com.example.lms.entity.Role.STUDENT);
        Course c1 = TestDataFactory.makeCourse(10L, "C1", "d", null, true);
        Enrollment e1 = TestDataFactory.makeEnrollment(21L, student, c1);

        when(enrollmentRepository.findByStudent(student)).thenReturn(List.of(e1));

        // act
        List<Enrollment> result = service.getEnrollmentsByStudent(student);

        // assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(e1, result.get(0));
        verify(enrollmentRepository, times(1)).findByStudent(student);
    }

    @Test
    void getEnrollmentsByCourse_returnsList() {
        // arrange
        User student = TestDataFactory.makeUser(4L, "bob", "bob@x.com", com.example.lms.entity.Role.STUDENT);
        Course course = TestDataFactory.makeCourse(20L, "Course20", "desc", null, true);
        Enrollment e = TestDataFactory.makeEnrollment(33L, student, course);

        when(enrollmentRepository.findByCourse(course)).thenReturn(List.of(e));

        // act
        List<Enrollment> result = service.getEnrollmentsByCourse(course);

        // assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(e, result.get(0));
        verify(enrollmentRepository, times(1)).findByCourse(course);
    }
}
