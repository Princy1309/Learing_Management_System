package com.example.lms.dto;

public class LessonCreateDto {
    private String title;
    private String contentType; // "video", "pdf", "image", "audio", "text"
    private String contentUrl; // S3 URL after upload
    private Integer lessonOrder;

    public LessonCreateDto() {
    }

    // getters + setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public Integer getLessonOrder() {
        return lessonOrder;
    }

    public void setLessonOrder(Integer lessonOrder) {
        this.lessonOrder = lessonOrder;
    }
}
