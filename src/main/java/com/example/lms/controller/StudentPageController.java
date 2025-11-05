package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StudentPageController {

    @GetMapping("/dashboard-student")
    public String studentDashboard() {
        return "student/dashboard-student";
    }

    @GetMapping("/mycourse")
    public String studentCourse() {
        return "student/my-course";
    }

    @GetMapping("/student/course/{id}")
    public String studentCoursePage(@PathVariable Long id) {
        return "student/course";
    }
}
