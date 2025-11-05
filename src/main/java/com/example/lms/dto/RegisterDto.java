package com.example.lms.dto;

import com.example.lms.entity.Role;
import lombok.Data;

@Data
public class RegisterDto {
    private String username;
    private String email;
    private String password;
    private Role role;
}