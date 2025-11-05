package com.example.lms.dto;

import com.example.lms.entity.Lesson;

public class LessonDto {
    private Long id;
    private String title;
    private String contentUrl;
    private String contentType;
    private boolean completed;
    private boolean accessible;
    private int lessonOrder;

    public static LessonDto fromEntity(Lesson lesson, boolean isCompleted, boolean isAccessible) {
        LessonDto dto = new LessonDto();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setContentUrl(lesson.getContentUrl());
        dto.setContentType(lesson.getContentType());
        dto.setCompleted(isCompleted);
        dto.setAccessible(isAccessible);
        dto.setLessonOrder(lesson.getLessonOrder());
        return dto;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
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

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getLessonOrder() {
        return lessonOrder;
    }

    public void setLessonOrder(int lessonOrder) {
        this.lessonOrder = lessonOrder;
    }
}
