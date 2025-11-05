package com.example.lms.controller;

import com.example.lms.dto.CourseCreateDto;
import com.example.lms.dto.CourseUpdateDto;
import com.example.lms.dto.StudentProgressDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import com.example.lms.repository.UserRepository;
import com.example.lms.service.CourseService;
import com.example.lms.service.CourseServiceImpl;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/instructor")
public class InstructorController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CourseServiceImpl courseServiceImpl;

    public InstructorController(CourseService courseService, UserRepository userRepository,
            CourseServiceImpl courseServiceImpl) {
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.courseServiceImpl = courseServiceImpl;
    }

    // Serve the create-course page (Thymeleaf)
    @GetMapping("create-course")
    public String createCoursePage() {
        return "instructor/create-course";
    }

    // to INSTRUCTOR)
    @PostMapping("/courses")
    @ResponseBody
    public ResponseEntity<?> createCourse(@RequestBody CourseCreateDto dto, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String email = authentication.getName(); // we use email as username in JWT/UserDetails
            User instructor = userRepository.findByEmail(email).orElse(null);
            if (instructor == null) {
                return ResponseEntity.status(400).body("Instructor not found");
            }

            com.example.lms.entity.Course saved = courseService.createCourseFromDto(dto, instructor);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create course: " + e.getMessage());
        }
    }

    @GetMapping("/manage-courses")
    public String manageCoursesPage() {
        return "instructor/manage-courses";
    }

    // 2. GET all courses for the instructor
    @GetMapping("/my-courses")
    @ResponseBody
    public ResponseEntity<?> getMyCourses(Authentication auth) {
        User instructor = userRepository.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(courseServiceImpl.getCoursesByInstructor(instructor));
    }

    @GetMapping("/courses/{id}")
    @ResponseBody
    public ResponseEntity<?> getCourseById(@PathVariable Long id, Authentication auth) {
        User instructor = userRepository.findByEmail(auth.getName()).orElseThrow();

        // Use the service to find the course
        Course course = courseServiceImpl.getCourseById(id);

        if (course == null) {
            return ResponseEntity.status(404).body("Course not found");
        }

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        return ResponseEntity.ok(course);
    }

    // 4. PUT to update a course
    @PutMapping("/courses/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody CourseUpdateDto dto,
            Authentication auth) {
        User instructor = userRepository.findByEmail(auth.getName()).orElseThrow();
        try {
            Course updatedCourse = courseServiceImpl.updateCourse(id, dto, instructor);
            return ResponseEntity.ok(updatedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // 5. DELETE a course (soft delete)
    @DeleteMapping("/courses/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCourse(@PathVariable Long id, Authentication auth) {
        User instructor = userRepository.findByEmail(auth.getName()).orElseThrow();
        try {
            courseServiceImpl.softDeleteCourse(id, instructor);
            return ResponseEntity.ok(Map.of("message", "Course deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // 6. POST to approve a course
    @PostMapping("/courses/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveCourse(@PathVariable Long id) {
        try {
            courseServiceImpl.approveCourse(id);
            return ResponseEntity.ok(Map.of("message", "Course approved."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/enrolled-students")
    public String enrolledStudentsPage() {
        return "instructor/enrolled-students"; // Path to your new Thymeleaf HTML file
    }

    // 2. API endpoint to get students for a specific course
    @GetMapping("/courses/{courseId}/enrolled-students")
    @ResponseBody
    public ResponseEntity<?> getEnrolledStudents(@PathVariable Long courseId, Authentication authentication) {
        try {
            User instructor = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("Instructor not found."));

            List<StudentProgressDto> students = courseServiceImpl.getEnrolledStudentsWithProgress(courseId, instructor);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
