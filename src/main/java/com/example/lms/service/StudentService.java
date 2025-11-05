package com.example.lms.service;

import com.example.lms.dto.MyCourseDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.Lesson;
import com.example.lms.entity.LessonProgress;
import com.example.lms.entity.User;
import com.example.lms.repository.CourseRepository;
import com.example.lms.repository.EnrollmentRepository;
import com.example.lms.repository.LessonProgressRepository;
import com.example.lms.repository.LessonRepository;
import com.example.lms.repository.UserRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    private final LessonRepository lessonRepository;

    public StudentService(CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository, LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;

    }

    public List<Course> getApprovedCourses() {
        // defensive: return empty list instead of null
        return courseRepository.findByApprovedTrue();
    }

    public void enrollStudent(Long courseId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("Course not found"));

        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalStateException("Already enrolled");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    public List<Enrollment> getEnrollments(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return enrollmentRepository.findByStudent(student);
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public boolean isStudentEnrolledInCourse(String studentEmail, Course course) {
        User student = userRepository.findByEmail(studentEmail).orElse(null);
        if (student == null)
            return false;
        return enrollmentRepository.existsByStudentAndCourse(student, course);
    }

    public void markLessonAsComplete(String username, Long lessonId) {
        // 1. Find the student
        User student = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found with email: " + username));

        // 2. Find the lesson
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalStateException("Lesson not found with ID: " + lessonId));

        // 3. Prevent duplicate entries
        lessonProgressRepository.findByStudentAndLesson(student, lesson).ifPresent(progress -> {
            throw new IllegalStateException("Lesson already marked as complete.");
        });

        // 4. Get the parent course from the lesson
        Course course = lesson.getCourse();
        Course nil = null;
        if (course == nil) {
            throw new IllegalStateException("Lesson is not associated with a course.");
        }

        // 5. Create and save the new progress record
        LessonProgress progress = new LessonProgress(student, lesson, course);
        lessonProgressRepository.save(progress);
    }

    public List<MyCourseDto> getMyCoursesWithProgress(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        List<MyCourseDto> coursesWithProgress = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();

            if (course != null) {
                int completedCount = lessonProgressRepository.countByStudentAndCourse(student, course);
                coursesWithProgress.add(MyCourseDto.fromEntity(course, completedCount));
            }

        }

        return coursesWithProgress;
    }
}
