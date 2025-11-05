package com.example.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.User;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentAndCourse(User student, Course course);

    List<Enrollment> findByStudent(User student);

    List<Enrollment> findByCourse(Course course);

    boolean existsByStudentAndCourse(User student, Course course);

}
