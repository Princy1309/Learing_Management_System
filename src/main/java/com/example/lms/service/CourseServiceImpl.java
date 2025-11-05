package com.example.lms.service;

import com.example.lms.repository.CourseRepository;
import com.example.lms.repository.EnrollmentRepository;
import com.example.lms.repository.LessonProgressRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import com.example.lms.dto.CourseCreateDto;
import com.example.lms.dto.CourseUpdateDto;
import com.example.lms.dto.LessonCreateDto;
import com.example.lms.dto.LessonDto;
import com.example.lms.dto.StudentProgressDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.Lesson;
import com.example.lms.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    private final LessonProgressRepository lessonProgressRepository;

    private final EnrollmentRepository enrollmentRepository;

    public CourseServiceImpl(CourseRepository courseRepository, LessonProgressRepository lessonProgressRepository,
            EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.enrollmentRepository = enrollmentRepository;

    }

    @Override
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> getCoursesByInstructor(User instructor) {
        return courseRepository.findByInstructor(instructor);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    public Course createCourseFromDto(CourseCreateDto dto, com.example.lms.entity.User instructor) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setInstructor(instructor);
        course.setApproved(false);

        if (dto.getLessons() != null) {
            int order = 1;
            for (LessonCreateDto ldto : dto.getLessons()) {
                Lesson lesson = new Lesson();
                lesson.setTitle(ldto.getTitle());
                lesson.setContentType(ldto.getContentType());
                lesson.setContentUrl(ldto.getContentUrl());
                lesson.setLessonOrder(ldto.getLessonOrder() != null ? ldto.getLessonOrder() : order++);
                lesson.setCourse(course); // set owning side
                course.getLessons().add(lesson);
            }
        }

        return courseRepository.save(course);
    }

    @Transactional
    public void softDeleteCourse(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("Course not found"));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new SecurityException("You are not authorized to delete this course.");
        }

        courseRepository.delete(course);
    }

    @Transactional
    public void approveCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("Course not found"));
        course.setApproved(true);
        courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long courseId, CourseUpdateDto dto, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("Course not found"));

        // Security check remains the same
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new SecurityException("You are not authorized to edit this course.");
        }

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());

        Map<Long, LessonDto> lessonDtoMap = dto.getLessons().stream()
                .collect(Collectors.toMap(LessonDto::getId, lesson -> lesson, (a, b) -> b));

        // 2. Identify lessons to be removed.
        List<Lesson> lessonsToRemove = new ArrayList<>();
        for (Lesson existingLesson : course.getLessons()) {
            if (!lessonDtoMap.containsKey(existingLesson.getId())) {
                lessonsToRemove.add(existingLesson);
            }
        }

        for (Lesson lesson : lessonsToRemove) {
            if (lessonProgressRepository.existsByLessonId(lesson.getId())) {
                // This is a user-friendly error that will be sent to the frontend.
                throw new IllegalStateException(
                        "Cannot remove lesson '" + lesson.getTitle() + "' because students have already completed it.");
            }
        }

        course.getLessons().removeAll(lessonsToRemove);

        for (LessonDto lessonDto : dto.getLessons()) {
            if (lessonDto.getId() != null) {
                course.getLessons().stream()
                        .filter(l -> l.getId().equals(lessonDto.getId()))
                        .findFirst()
                        .ifPresent(lesson -> {
                            lesson.setTitle(lessonDto.getTitle());
                            lesson.setContentType(lessonDto.getContentType());
                            lesson.setContentUrl(lessonDto.getContentUrl());
                            lesson.setLessonOrder(lessonDto.getLessonOrder());
                        });
            } else {
                // This is a new lesson (ID is null), create and add it.
                course.getLessons().add(new Lesson(
                        lessonDto.getTitle(),
                        lessonDto.getContentType(),
                        lessonDto.getContentUrl(),
                        lessonDto.getLessonOrder(),
                        course));
            }
        }

        return courseRepository.save(course);
    }

    public List<StudentProgressDto> getEnrolledStudentsWithProgress(Long courseId, User instructor) {
        // 1. Find the course and verify ownership
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("Course not found."));

        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new SecurityException("You are not authorized to view students for this course.");
        }

        // 2. Get all enrollments for this course
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        int totalLessons = course.getLessons().size();

        // 3. For each enrollment, create a DTO with progress
        List<StudentProgressDto> studentProgressList = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            int completedCount = lessonProgressRepository.countByStudentAndCourse(student, course);
            studentProgressList.add(StudentProgressDto.fromEntity(student, completedCount, totalLessons));
        }

        return studentProgressList;
    }
}
