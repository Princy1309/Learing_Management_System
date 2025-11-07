package com.example.lms.util;

import com.example.lms.entity.Course;
import com.example.lms.entity.Enrollment;
import com.example.lms.entity.Lesson;
import com.example.lms.entity.LessonProgress;
import com.example.lms.entity.Role;
import com.example.lms.entity.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class TestDataFactory {

    // ---------- Reflection helper ----------
    private static void setField(Object target, String fieldName, Object value) {
        if (target == null)
            return;
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return; // success
            } catch (NoSuchFieldException nsfe) {
                // try superclass
                cls = cls.getSuperclass();
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("Failed to set field '" + fieldName + "' on " + target.getClass(), iae);
            }
        }
        // If field not found, quietly return (useful when entity uses different naming)
    }

    // ---------- User ----------
    public static User makeUser(Long id, String name, String email, Role role) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPassword("test-pass");
        user.setRole(role);
        user.setEnabled(true);

        // set id via reflection if setter missing
        setField(user, "id", id);
        return user;
    }

    // ---------- Course ----------
    public static Course makeCourse(Long id, String title, String description, User instructor, boolean published) {
        Course course = new Course();
        // try to use setters if present
        try {
            course.setTitle(title);
        } catch (Throwable ignored) {
        }
        try {
            course.setDescription(description);
        } catch (Throwable ignored) {
        }
        try {
            course.setInstructor(instructor);
        } catch (Throwable ignored) {
        }

        // Set id and either 'published' or 'approved' (whichever exists) via reflection
        setField(course, "id", id);
        setField(course, "published", published);
        setField(course, "approved", published);

        return course;
    }

    // ---------- Enrollment ----------
    public static Enrollment makeEnrollment(Long id, User student, Course course) {
        Enrollment e = new Enrollment();
        try {
            e.setStudent(student);
        } catch (Throwable ignored) {
        }
        try {
            e.setCourse(course);
        } catch (Throwable ignored) {
        }
        try {
            e.setEnrolledAt(LocalDateTime.now());
        } catch (Throwable ignored) {
        }

        setField(e, "id", id);
        return e;
    }

    // ---------- Lesson ----------
    public static Lesson makeLesson(Long id, String title, Course course) {
        Lesson l = new Lesson();
        try {
            l.setTitle(title);
        } catch (Throwable ignored) {
        }
        try {
            l.setCourse(course);
        } catch (Throwable ignored) {
        }
        setField(l, "id", id);
        return l;
    }

    // ---------- LessonProgress ----------
    public static LessonProgress makeLessonProgress(Long id, User student, Lesson lesson, Course course) {
        LessonProgress lp = new LessonProgress(student, lesson, course);
        setField(lp, "id", id);
        return lp;
    }
}
