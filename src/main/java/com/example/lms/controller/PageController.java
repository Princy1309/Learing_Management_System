package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Home Page (default entry)
    @GetMapping({ "/", "/home", "/home-page" })
    public String homePage() {
        return "home"; // corresponds to home.html
    }

    // Registration Page
    @GetMapping("/register")
    public String registerPage() {
        return "register"; // corresponds to register.html
    }

    // Login Page
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // corresponds to login.html
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied"; // optional, for forbidden access
    }
}
