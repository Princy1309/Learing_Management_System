package com.example.lms.service;

import java.util.List;

import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.User;

public interface EnrollmentService {
    Enrollment enrollStudent(User student, Course course);

    List<Enrollment> getEnrollmentsByStudent(User student);

    List<Enrollment> getEnrollmentsByCourse(Course course);
}
