package com.example.lms.controller;

import com.example.lms.dto.RegisterDto;
import com.example.lms.dto.RoleUpdateDto;
import com.example.lms.entity.Course;
import com.example.lms.entity.User;
import com.example.lms.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- User Endpoints ---
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return adminService.findAllUsers();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDto registerDto) {
        try {
            adminService.registerUser(registerDto);
            return ResponseEntity.ok(Map.of("message", "User created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateDto dto) {
        adminService.updateUserRole(id, dto.getRole());
        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // --- Course Endpoints ---
    @GetMapping("/courses/pending")
    public List<Course> getPendingCourses() {
        return adminService.findPendingCourses();
    }

    @PutMapping("/courses/{id}/approve")
    public ResponseEntity<?> approveCourse(@PathVariable Long id) {
        adminService.approveCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course approved"));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> removeCourse(@PathVariable Long id) {
        adminService.removeCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course removed"));
    }
}