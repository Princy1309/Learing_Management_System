package com.example.lms.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import com.example.lms.entity.Lesson;
import com.example.lms.repository.LessonProgressRepository;

public class CourseDto {
    public Long id;
    public String title;
    public String description;
    public boolean approved;
    public LocalDateTime createdAt;
    public Long instructorId;
    public String instructorUsername;
    public List<LessonDto> lessons;

    public static CourseDto fromEntity(Course course, User student, LessonProgressRepository progressRepo) {
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setLessons(new ArrayList<>()); // Initialize the list

        if (course.getLessons() == null || course.getLessons().isEmpty()) {
            return dto; // Return early if there are no lessons
        }
        Set<Long> completedLessonIds = progressRepo.findByStudentAndLessonIn(student, course.getLessons())
                .stream()
                .map(progress -> progress.getLesson().getId())
                .collect(Collectors.toSet());

        List<Lesson> sortedLessons = course.getLessons().stream()
                .sorted(Comparator.comparing(Lesson::getLessonOrder))
                .collect(Collectors.toList());

        boolean previousLessonCompleted = true;

        for (Lesson lesson : sortedLessons) {
            boolean isCompleted = completedLessonIds.contains(lesson.getId());
            boolean isAccessible = previousLessonCompleted;

            dto.getLessons().add(LessonDto.fromEntity(lesson, isCompleted, isAccessible));

            // The next lesson is only accessible if THIS one is completed
            previousLessonCompleted = isCompleted;
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorUsername() {
        return instructorUsername;
    }

    public void setInstructorUsername(String instructorUsername) {
        this.instructorUsername = instructorUsername;
    }

    public List<LessonDto> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonDto> lessons) {
        this.lessons = lessons;
    }

}
