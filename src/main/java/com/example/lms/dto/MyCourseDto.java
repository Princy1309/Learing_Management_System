package com.example.lms.dto;

import com.example.lms.entity.Course;

public class MyCourseDto {
    private Long id;
    private String title;
    private String description;
    private int totalLessons;
    private int completedLessons;

    public static MyCourseDto fromEntity(Course course, int completedLessons) {
        MyCourseDto dto = new MyCourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setTotalLessons(course.getLessons() != null ? course.getLessons().size() : 0);
        dto.setCompletedLessons(completedLessons);
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }
}