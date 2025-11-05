package com.example.lms.service;

import com.example.lms.dto.CourseCreateDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.User;

import java.util.List;

public interface CourseService {
    Course createCourse(Course course);

    List<Course> getAllCourses();

    List<Course> getCoursesByInstructor(User instructor);

    Course getCourseById(Long id);

    Course createCourseFromDto(CourseCreateDto dto, com.example.lms.entity.User instructor);
}
