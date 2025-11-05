package com.example.lms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.lms.entity.Course;
import com.example.lms.entity.Lesson;
import com.example.lms.entity.LessonProgress;
import com.example.lms.entity.User;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByStudentAndLesson(User student, Lesson lesson);

    List<LessonProgress> findByStudentAndLessonIn(User student, List<Lesson> lessons);

    int countByStudentAndCourse(User student, Course course);

    boolean existsByLessonId(Long lessonId);
}
