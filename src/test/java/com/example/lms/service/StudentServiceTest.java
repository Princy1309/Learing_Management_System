package com.example.lms.service;

import com.example.lms.dto.MyCourseDto;
import com.example.lms.entity.*;
import com.example.lms.repository.*;
import com.example.lms.util.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private StudentService studentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void enroll_whenAlreadyEnrolled_throws() {
        User s = TestDataFactory.makeUser(1L, "stu", "s@x.com",
                com.example.lms.entity.Role.STUDENT);
        Course c = TestDataFactory.makeCourse(10L, "t", "d", null, false);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> studentService.enrollStudent(10L, "s@x.com"));
    }

    @Test
    void enroll_success_callsSave() {
        User s = TestDataFactory.makeUser(1L, "stu", "s@x.com",
                com.example.lms.entity.Role.STUDENT);
        Course c = TestDataFactory.makeCourse(10L, "t", "d", null, false);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        studentService.enrollStudent(10L, "s@x.com");
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // --- small helpers (you can move these to TestDataFactory if you want) ---
    private static User makeUser(Long id, String name, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        u.setEmail(email);
        u.setPassword("pw");
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    private static Course makeCourse(Long id, String title, String desc, User instructor, boolean published) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        c.setDescription(desc);
        c.setInstructor(instructor);
        // c.setPublished(published);
        c.setApproved(true);
        return c;
    }

    private static Lesson makeLesson(Long id, String title, Course course) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setTitle(title);
        l.setCourse(course);
        return l;
    }

    private static Enrollment makeEnrollment(Long id, User student, Course course) {
        Enrollment e = new Enrollment();
        e.setId(id);
        e.setStudent(student);
        e.setCourse(course);
        e.setEnrolledAt(LocalDateTime.now());
        return e;
    }

    private static LessonProgress makeLessonProgress(Long id, User student, Lesson lesson, Course course) {
        LessonProgress lp = new LessonProgress(student, lesson, course);
        lp.setId(id);
        return lp;
    }

    @Test
    void getApprovedCourses_returnsList() {
        Course c1 = makeCourse(1L, "t1", "d1", null, true);
        Course c2 = makeCourse(2L, "t2", "d2", null, true);
        when(courseRepository.findByApprovedTrue()).thenReturn(List.of(c1, c2));

        var res = studentService.getApprovedCourses();
        assertNotNull(res);
        assertEquals(2, res.size());
        verify(courseRepository, times(1)).findByApprovedTrue();
    }

    @Test
    void enrollStudent_alreadyEnrolled_throws() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c = makeCourse(10L, "t", "d", null, false);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> studentService.enrollStudent(10L, "s@x.com"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_success_savesEnrollment() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c = makeCourse(10L, "t", "d", null, false);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(99L);
            return e;
        });

        studentService.enrollStudent(10L, "s@x.com");

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository, times(1)).save(captor.capture());
        Enrollment saved = captor.getValue();

        assertEquals(s, saved.getStudent());
        assertEquals(c, saved.getCourse());
        assertNotNull(saved.getEnrolledAt());
    }

    @Test
    void enrollStudent_userNotFound_throws() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> studentService.enrollStudent(1L, "no@x.com"));
    }

    @Test
    void enrollStudent_courseNotFound_throws() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(courseRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> studentService.enrollStudent(5L, "s@x.com"));
    }

    @Test
    void getEnrollments_returnsEnrollments() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Enrollment e1 = makeEnrollment(11L, s, makeCourse(10L, "c1", "d", null, true));
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(enrollmentRepository.findByStudent(s)).thenReturn(List.of(e1));

        var res = studentService.getEnrollments("s@x.com");
        assertEquals(1, res.size());
        assertSame(e1, res.get(0));
    }

    @Test
    void getEnrollments_userNotFound_throws() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> studentService.getEnrollments("no@x.com"));
    }

    @Test
    void getCourseById_present_and_absent() {
        Course c = makeCourse(7L, "title", "desc", null, true);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(c));
        when(courseRepository.findById(8L)).thenReturn(Optional.empty());

        assertTrue(studentService.getCourseById(7L).isPresent());
        assertFalse(studentService.getCourseById(8L).isPresent());
    }

    @Test
    void isStudentEnrolledInCourse_studentMissing_returnsFalse() {
        Course course = makeCourse(1L, "t", "d", null, true);
        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        assertFalse(studentService.isStudentEnrolledInCourse("missing@x.com", course));
    }

    @Test
    void isStudentEnrolledInCourse_trueAndFalse() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c = makeCourse(10L, "t", "d", null, false);
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(true);
        assertTrue(studentService.isStudentEnrolledInCourse("s@x.com", c));

        when(enrollmentRepository.existsByStudentAndCourse(s, c)).thenReturn(false);
        assertFalse(studentService.isStudentEnrolledInCourse("s@x.com", c));
    }

    @Test
    void markLessonAsComplete_success_savesProgress() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c = makeCourse(100L, "c", "d", null, true);
        Lesson lesson = makeLesson(50L, "L1", c);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(lessonRepository.findById(50L)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentAndLesson(s, lesson)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(i -> i.getArgument(0));

        studentService.markLessonAsComplete("s@x.com", 50L);

        ArgumentCaptor<LessonProgress> captor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(lessonProgressRepository, times(1)).save(captor.capture());
        LessonProgress saved = captor.getValue();
        assertEquals(s, saved.getStudent());
        assertEquals(lesson, saved.getLesson());
        assertEquals(c, saved.getCourse());
    }

    @Test
    void markLessonAsComplete_studentNotFound_throws() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> studentService.markLessonAsComplete("no@x.com", 1L));
    }

    @Test
    void markLessonAsComplete_lessonNotFound_throws() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> studentService.markLessonAsComplete("s@x.com", 99L));
    }

    @Test
    void markLessonAsComplete_alreadyMarked_throws() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c = makeCourse(200L, "c", "d", null, true);
        Lesson lesson = makeLesson(60L, "L", c);
        LessonProgress existing = makeLessonProgress(1L, s, lesson, c);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(lessonRepository.findById(60L)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentAndLesson(s, lesson)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> studentService.markLessonAsComplete("s@x.com", 60L));
        verify(lessonProgressRepository, never()).save(any());
    }

    @Test
    void markLessonAsComplete_lessonWithoutCourse_throws() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Lesson lesson = makeLesson(77L, "L-no-course", null);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(lessonRepository.findById(77L)).thenReturn(Optional.of(lesson));

        assertThrows(IllegalStateException.class, () -> studentService.markLessonAsComplete("s@x.com", 77L));
        verify(lessonProgressRepository, never()).save(any());
    }

    // ------------- getMyCoursesWithProgress --------------
    @Test
    void getMyCoursesWithProgress_returnsDtosAndSkipsNullCourses() {
        User s = makeUser(1L, "stu", "s@x.com", Role.STUDENT);
        Course c1 = makeCourse(10L, "C1", "d", null, true);
        Course c2 = makeCourse(20L, "C2", "d", null, true);
        Enrollment e1 = makeEnrollment(1L, s, c1);
        Enrollment e2 = makeEnrollment(2L, s, c2);
        Enrollment eNull = makeEnrollment(3L, s, null);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(s));
        when(enrollmentRepository.findByStudent(s)).thenReturn(List.of(e1, e2, eNull));

        when(lessonProgressRepository.countByStudentAndCourse(s, c1)).thenReturn(2);
        when(lessonProgressRepository.countByStudentAndCourse(s, c2)).thenReturn(0);

        List<MyCourseDto> dtos = studentService.getMyCoursesWithProgress("s@x.com");
        assertEquals(2, dtos.size());

        verify(lessonProgressRepository, times(1)).countByStudentAndCourse(s, c1);
        verify(lessonProgressRepository, times(1)).countByStudentAndCourse(s, c2);
    }

    @Test
    void getMyCoursesWithProgress_userNotFound_throws() {
        when(userRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> studentService.getMyCoursesWithProgress("no@x.com"));
    }
}
