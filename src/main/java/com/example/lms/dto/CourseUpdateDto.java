
package com.example.lms.dto;

import java.util.List;

public class CourseUpdateDto {
    private String title;
    private String description;
    private List<LessonDto> lessons;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public List<LessonDto> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonDto> lessons) {
        this.lessons = lessons;
    }
}