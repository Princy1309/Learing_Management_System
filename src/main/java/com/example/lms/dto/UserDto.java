package com.example.lms.dto;

import com.example.lms.entity.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String username;
    private String email;
    private String password;
    private Role role;
}
