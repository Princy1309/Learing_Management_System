// package com.example.lms.service;

// import com.example.lms.entity.Course;
// import com.example.lms.entity.Enrollment;
// import com.example.lms.entity.User;
// import com.example.lms.repository.CourseRepository;
// import com.example.lms.repository.EnrollmentRepository;
// import com.example.lms.repository.UserRepository;
// import com.example.lms.service.StudentService;

// import org.junit.jupiter.api.Test;
// import org.mockito.Mockito;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import static org.mockito.ArgumentMatchers.any;
// import static org.junit.jupiter.api.Assertions.*;

// public class StudentServiceTest {

// @MockitoBean
// private StudentService studentService;

// @Test
// void enrollStudent_savesEnrollment_whenNotAlreadyEnrolled() {
// CourseRepository courseRepo = Mockito.mock(CourseRepository.class);
// UserRepository userRepo = Mockito.mock(UserRepository.class);
// EnrollmentRepository enrollmentRepo =
// Mockito.mock(EnrollmentRepository.class);

// StudentService studentService = new StudentService(courseRepo, userRepo,
// enrollmentRepo);

// User user = new User();
// user.setEmail("t@t.com");

// Course course = new Course();

// Mockito.when(userRepo.findByEmail("t@t.com")).thenReturn(Optional.of(user));
// Mockito.when(courseRepo.findById(1L)).thenReturn(Optional.of(course));
// Mockito.when(enrollmentRepo.existsByStudentAndCourse(user,
// course)).thenReturn(false);

// studentService.enrollStudent(1L, "t@t.com");

// Mockito.verify(enrollmentRepo).save(any(Enrollment.class));
// }

// @Test
// void getEnrollments_returnsEnrollments_forStudent() {
// CourseRepository courseRepo = Mockito.mock(CourseRepository.class);
// UserRepository userRepo = Mockito.mock(UserRepository.class);
// EnrollmentRepository enrollmentRepo =
// Mockito.mock(EnrollmentRepository.class);

// StudentService studentService = new StudentService(courseRepo, userRepo,
// enrollmentRepo);

// User user = new User();
// user.setEmail("t@t.com");

// Enrollment e = new Enrollment();
// e.setEnrolledAt(LocalDateTime.now());
// Mockito.when(userRepo.findByEmail("t@t.com")).thenReturn(Optional.of(user));
// Mockito.when(enrollmentRepo.findByStudent(user)).thenReturn(List.of(e));

// var list = studentService.getEnrollments("t@t.com");
// assertNotNull(list);
// assertEquals(1, list.size());
// assertEquals(e, list.get(0));
// }
// }
