package com.example.lms.service;

import com.example.lms.dto.RegisterDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;
import com.example.lms.repository.CourseRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuthService authService;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public String registerUser(RegisterDto dto) {
        return authService.register(
                dto.getUsername(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getRole());
    }

    // --- Course Management ---
    public List<Course> findPendingCourses() {
        return courseRepository.findByApprovedFalseAndIsDeletedFalse();
    }

    @Transactional
    public void approveCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setApproved(true);
        courseRepository.save(course);
    }

    @Transactional
    public void removeCourse(Long courseId) {
        // This is a hard delete, suitable for a non-approved course.
        courseRepository.deleteById(courseId);
    }
}