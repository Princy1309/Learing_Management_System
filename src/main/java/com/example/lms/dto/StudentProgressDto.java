package com.example.lms.dto;

import com.example.lms.entity.User;

public class StudentProgressDto {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private int completedLessons;
    private int totalLessons;

    public static StudentProgressDto fromEntity(User student, int completedLessons, int totalLessons) {
        StudentProgressDto dto = new StudentProgressDto();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getUsername()); // Or getFullName() if you have it
        dto.setStudentEmail(student.getEmail());
        dto.setCompletedLessons(completedLessons);
        dto.setTotalLessons(totalLessons);
        return dto;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }
}