package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InstructorPageController {

    @GetMapping("/dashboard-instructor")
    public String instructorDashboard() {
        return "instructor/dashboard";
    }

    @GetMapping("/instructor/create-course")
    public String createCoursePage() {
        return "instructor/dashboard";
    }

    @GetMapping("/instructor/update-course")
    public String updateCoursePage() {
        return "instructor/manage-courses";
    }

    @GetMapping("/instructor/enrolled-students")
    public String enrolledStudentsPage() {
        return "instructor/enrolled-students";
    }
}
