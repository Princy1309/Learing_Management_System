package com.example.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructor(User instructor);

    List<Course> findByApprovedTrue();

    List<Course> findByApprovedFalseAndIsDeletedFalse();
}
