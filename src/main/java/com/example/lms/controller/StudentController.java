package com.example.lms.controller;

import com.example.lms.dto.CourseDto;
import com.example.lms.dto.MyCourseDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.User;
import com.example.lms.service.StudentService;

import org.springframework.http.ResponseEntity;
import com.example.lms.repository.UserRepository;
import com.example.lms.repository.LessonProgressRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public StudentController(StudentService studentService,
            UserRepository userRepository,
            LessonProgressRepository lessonProgressRepository) {
        this.studentService = studentService;
        this.userRepository = userRepository;
        this.lessonProgressRepository = lessonProgressRepository;

    }

    // Public browse of approved courses
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getApprovedCourses() {
        try {
            List<Course> courses = studentService.getApprovedCourses();
            return ResponseEntity.ok(courses);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    // Enroll (requires authentication)
    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<?> enrollInCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            studentService.enrollStudent(courseId, userDetails.getUsername());
            return ResponseEntity.ok("Enrolled successfully");
        } catch (IllegalStateException ise) {
            return ResponseEntity.badRequest().body(ise.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Enrollment failed");
        }
    }

    // My enrollments
    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).build();
            }
            return ResponseEntity.ok(studentService.getEnrollments(userDetails.getUsername()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourseForStudent(@PathVariable Long id, Principal principal) {
        try {
            // 1. Find the course
            Course course = studentService.getCourseById(id)
                    .orElseThrow(() -> new IllegalStateException("Course not found"));

            // 2. Ensure a user is logged in
            if (principal == null) {
                return ResponseEntity.status(403).body("You must be logged in to view this course");
            }

            // 3. Find the full student entity
            User student = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // 4. (Optional but recommended) Check if the student is enrolled
            boolean enrolled = studentService.isStudentEnrolledInCourse(principal.getName(), course);
            if (!enrolled) {
                return ResponseEntity.status(403).body("You are not enrolled in this course");
            }

            // It passes the student and the repository to calculate progress.
            CourseDto courseDto = CourseDto.fromEntity(course, student, lessonProgressRepository);

            return ResponseEntity.ok(courseDto);

        } catch (IllegalStateException | UsernameNotFoundException ise) {
            // Handles "Course not found" or "User not found"
            return ResponseEntity.status(404).body(Map.of("error", ise.getMessage()));
        } catch (Exception ex) {
            // Catches any other unexpected server errors
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred on the server."));
        }
    }

    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {

            List<MyCourseDto> myCourses = studentService.getMyCoursesWithProgress(userDetails.getUsername());
            return ResponseEntity.ok(myCourses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Something went wrong");
        }
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<?> markLessonAsComplete(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized: You must be logged in.");
        }

        try {
            // Delegate the logic to the service layer
            studentService.markLessonAsComplete(userDetails.getUsername(), lessonId);
            return ResponseEntity.ok().body(Map.of("message", "Lesson marked as complete."));
        } catch (IllegalStateException | UsernameNotFoundException e) {
            // Handle specific, expected errors (like "not found" or "already complete")
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected server errors
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

}
