package com.example.lms.dto;

import java.util.List;

public class CourseCreateDto {
    private String title;
    private String description;
    private List<LessonCreateDto> lessons;

    public CourseCreateDto() {
    }

    // getters & setters
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

    public List<LessonCreateDto> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonCreateDto> lessons) {
        this.lessons = lessons;
    }
}
