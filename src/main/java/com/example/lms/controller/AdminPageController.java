package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/dashboard-admin")
    public String adminDashboard() {
        return "admin/dashboard-admin";
    }
}
