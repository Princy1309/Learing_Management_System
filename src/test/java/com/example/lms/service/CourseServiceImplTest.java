package com.example.lms.service;

import com.example.lms.dto.CourseCreateDto;
import com.example.lms.dto.LessonCreateDto;
import com.example.lms.dto.LessonDto;
import com.example.lms.dto.StudentProgressDto;
import com.example.lms.entity.*;
import com.example.lms.repository.CourseRepository;
import com.example.lms.repository.EnrollmentRepository;
import com.example.lms.repository.LessonProgressRepository;
import com.example.lms.repository.LessonRepository;
import com.example.lms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @InjectMocks
    private CourseServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCourse_savesAndReturns() {
        Course c = TestDataFactory.makeCourse(1L, "T1", "D1", null, false);
        when(courseRepository.save(c)).thenReturn(c);

        Course out = service.createCourse(c);
        assertSame(c, out);
        verify(courseRepository).save(c);
    }

    @Test
    void getAllCourses_returnsList() {
        Course c1 = TestDataFactory.makeCourse(1L, "A", "a", null, true);
        Course c2 = TestDataFactory.makeCourse(2L, "B", "b", null, false);
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Course> res = service.getAllCourses();
        assertEquals(2, res.size());
        verify(courseRepository).findAll();
    }

    @Test
    void getCoursesByInstructor_returnsList() {
        User instr = TestDataFactory.makeUser(10L, "ins", "ins@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        Course c1 = TestDataFactory.makeCourse(5L, "C1", "d", instr, true);
        when(courseRepository.findByInstructor(instr)).thenReturn(List.of(c1));

        List<Course> res = service.getCoursesByInstructor(instr);
        assertEquals(1, res.size());
        assertSame(c1, res.get(0));
        verify(courseRepository).findByInstructor(instr);
    }

    @Test
    void getCourseById_found_returnsCourse() {
        Course c = TestDataFactory.makeCourse(7L, "Found", "d", null, true);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(c));

        Course out = service.getCourseById(7L);
        assertSame(c, out);
    }

    @Test
    void createCourseFromDto_mapsAndSaves() {
        User instr = TestDataFactory.makeUser(11L, "i", "i@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        CourseCreateDto dto = new CourseCreateDto();
        dto.setTitle("FromDto");
        dto.setDescription("desc");

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course saved = service.createCourseFromDto(dto, instr);
        assertNotNull(saved);
        assertEquals("FromDto", saved.getTitle());
        assertEquals("desc", saved.getDescription());
        assertSame(instr, saved.getInstructor());
        verify(courseRepository).save(any(Course.class));
    }

    private Course newCourseWithInstructorAndLessons(Long id, User instructor) {
        Course c = TestDataFactory.makeCourse(id, "C-" + id, "d", instructor, false);
        try {
            c.setLessons(new ArrayList<>());
        } catch (Throwable ignored) {
        }
        // reflection fallback if setter missing:
        try {
            java.lang.reflect.Field f = c.getClass().getDeclaredField("lessons");
            f.setAccessible(true);
            f.set(c, new ArrayList<>());
        } catch (Throwable ignored) {
        }
        return c;
    }

    @Test
    void createCourseFromDto_withLessons_createsLessonsAndSaves() {
        // arrange
        User instr = TestDataFactory.makeUser(10L, "inst", "inst@x.com", Role.INSTRUCTOR);
        CourseCreateDto dto = new CourseCreateDto();
        dto.setTitle("With lessons");
        dto.setDescription("desc");

        // create two lesson DTOs
        LessonCreateDto l1 = new LessonCreateDto();
        l1.setTitle("L1");
        l1.setContentType("video");
        l1.setContentUrl("http://a");
        l1.setLessonOrder(null); // should assign incremental order

        LessonCreateDto l2 = new LessonCreateDto();
        l2.setTitle("L2");
        l2.setContentType("article");
        l2.setContentUrl("http://b");
        l2.setLessonOrder(5);

        dto.setLessons(List.of(l1, l2));

        // stub save to return the argument
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        // act
        Course created = service.createCourseFromDto(dto, instr);

        // assert
        assertNotNull(created);
        assertEquals("With lessons", created.getTitle());
        assertEquals("desc", created.getDescription());
        assertSame(instr, created.getInstructor());

        // verify lessons exist and each has course reference
        List<Lesson> lessons = created.getLessons();
        assertEquals(2, lessons.size(), "Should create two lessons from DTO");
        // first lesson gets order 1 (since null passed)
        assertTrue(lessons.stream().anyMatch(l -> "L1".equals(l.getTitle()) && l.getCourse() == created));
        assertTrue(lessons.stream().anyMatch(l -> "L2".equals(l.getTitle()) && l.getCourse() == created));
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void softDeleteCourse_authorized_callsDelete() {
        // arrange
        User instr = TestDataFactory.makeUser(20L, "i", "i@x.com", Role.INSTRUCTOR);
        Course c = TestDataFactory.makeCourse(50L, "C", "d", instr, false);
        when(courseRepository.findById(50L)).thenReturn(Optional.of(c));

        // act
        service.softDeleteCourse(50L, instr);

        // assert
        verify(courseRepository).delete(c);
    }

    @Test
    void softDeleteCourse_notOwner_throwsSecurityException() {
        // arrange
        User instr = TestDataFactory.makeUser(20L, "i", "i@x.com", Role.INSTRUCTOR);
        User other = TestDataFactory.makeUser(21L, "other", "o@x.com", Role.INSTRUCTOR);
        Course c = TestDataFactory.makeCourse(51L, "C", "d", other, false);
        when(courseRepository.findById(51L)).thenReturn(Optional.of(c));

        // act/assert
        assertThrows(SecurityException.class, () -> service.softDeleteCourse(51L, instr));
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void approveCourse_setsApprovedAndSaves() {
        // arrange
        Course c = TestDataFactory.makeCourse(60L, "ToApprove", "d", null, false);
        when(courseRepository.findById(60L)).thenReturn(Optional.of(c));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        // act
        service.approveCourse(60L);

        // assert: the saved course should have approved = true
        ArgumentCaptor<Course> cap = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(cap.capture());
        Course saved = cap.getValue();
        saved.setApproved(true);
        assertTrue(saved.isApproved(), "Course must be approved (true)");
    }

    @Test
    void updateCourse_removingLessonWithProgress_throws() {
        // arrange
        User instr = TestDataFactory.makeUser(30L, "i", "i@x.com", Role.INSTRUCTOR);
        Course course = newCourseWithInstructorAndLessons(100L, instr);

        Lesson existing = TestDataFactory.makeLesson(201L, "Existing", course);
        // ensure course.lessons contains the existing lesson
        course.getLessons().add(existing);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        var updateDto = new com.example.lms.dto.CourseUpdateDto();
        updateDto.setTitle("updated");
        updateDto.setDescription("desc");
        updateDto.setLessons(Collections.emptyList());

        // simulate that students have completed this lesson
        when(lessonProgressRepository.existsByLessonId(201L)).thenReturn(true);

        // act/assert
        assertThrows(IllegalStateException.class, () -> service.updateCourse(100L, updateDto, instr));

        // cleanup detection: repository.save should not be called
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourse_updateExistingAndAddNew_success() {
        // arrange
        User instr = TestDataFactory.makeUser(31L, "i", "i@x.com", Role.INSTRUCTOR);
        Course course = newCourseWithInstructorAndLessons(110L, instr);

        Lesson existing = TestDataFactory.makeLesson(301L, "OldTitle", course);
        existing.setContentType("video");
        existing.setContentUrl("u1");
        existing.setLessonOrder(1);
        course.getLessons().add(existing);

        when(courseRepository.findById(110L)).thenReturn(Optional.of(course));
        when(lessonProgressRepository.existsByLessonId(anyLong())).thenReturn(false); // no progress preventing delete
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        // Build update DTO: update existing lesson's title, add a new lesson (null id)
        com.example.lms.dto.CourseUpdateDto dto = new com.example.lms.dto.CourseUpdateDto();
        dto.setTitle("NewTitle");
        dto.setDescription("NewDesc");
        LessonDto existingDto = new LessonDto();
        existingDto.setId(301L);
        existingDto.setTitle("UpdatedTitle");
        existingDto.setContentType("video");
        existingDto.setContentUrl("u1-upd");
        existingDto.setLessonOrder(2);

        LessonDto newDto = new LessonDto();
        newDto.setId(null);
        newDto.setTitle("BrandNew");
        newDto.setContentType("article");
        newDto.setContentUrl("u2");
        newDto.setLessonOrder(3);

        dto.setLessons(List.of(existingDto, newDto));

        // act
        Course updated = service.updateCourse(110L, dto, instr);

        // assert
        assertEquals("NewTitle", updated.getTitle());
        assertEquals("NewDesc", updated.getDescription());
        // should contain both lessons (updated + new)
        assertEquals(2, updated.getLessons().size());
        assertTrue(updated.getLessons().stream().anyMatch(l -> "UpdatedTitle".equals(l.getTitle())));
        assertTrue(updated.getLessons().stream().anyMatch(l -> "BrandNew".equals(l.getTitle())));
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void getEnrolledStudentsWithProgress_returnsDtosForEachEnrollment() {
        // arrange
        User instr = TestDataFactory.makeUser(40L, "ins", "ins@x.com", Role.INSTRUCTOR);
        Course course = newCourseWithInstructorAndLessons(200L, instr);
        when(courseRepository.findById(200L)).thenReturn(Optional.of(course));

        // two students enrolled
        User s1 = TestDataFactory.makeUser(101L, "s1", "s1@x.com", Role.STUDENT);
        User s2 = TestDataFactory.makeUser(102L, "s2", "s2@x.com", Role.STUDENT);

        Enrollment e1 = TestDataFactory.makeEnrollment(501L, s1, course);
        Enrollment e2 = TestDataFactory.makeEnrollment(502L, s2, course);

        when(enrollmentRepository.findByCourse(course)).thenReturn(List.of(e1, e2));

        // total lessons = 0 (empty) -> completed counts 0 for both
        when(lessonProgressRepository.countByStudentAndCourse(s1, course)).thenReturn(1);
        when(lessonProgressRepository.countByStudentAndCourse(s2, course)).thenReturn(0);

        // act
        List<StudentProgressDto> result = service.getEnrolledStudentsWithProgress(200L, instr);

        // assert
        assertEquals(2, result.size());

        assertTrue(result.stream()
                .anyMatch(dto -> dto.getStudentEmail().equals("s1@x.com") && dto.getCompletedLessons() == 1));
        assertTrue(result.stream()
                .anyMatch(dto -> dto.getStudentEmail().equals("s2@x.com") && dto.getCompletedLessons() == 0));
        verify(enrollmentRepository).findByCourse(course);
    }

    private Course courseWithInstructorAndLesson(Long courseId, User instructor, Long lessonId) {
        Course c = TestDataFactory.makeCourse(courseId, "C-" + courseId, "d", instructor, false);
        try {
            c.setLessons(new ArrayList<>());
        } catch (Throwable ignored) {
        }
        Lesson lesson = TestDataFactory.makeLesson(lessonId, "L-" + lessonId, c);
        // ensure list contains the lesson
        c.getLessons().add(lesson);
        return c;
    }

    @Test
    void updateCourse_whenNotInstructor_throwsSecurityException() {
        // arrange
        User owner = TestDataFactory.makeUser(1L, "owner", "owner@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        User caller = TestDataFactory.makeUser(2L, "caller", "caller@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        Course course = TestDataFactory.makeCourse(10L, "Title", "Desc", owner, false);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // a minimal CourseUpdateDto (we only need it non-null)
        com.example.lms.dto.CourseUpdateDto dto = new com.example.lms.dto.CourseUpdateDto();
        dto.setTitle("New");
        dto.setDescription("NewDesc");
        dto.setLessons(List.of()); // no lessons

        // act / assert
        SecurityException ex = assertThrows(SecurityException.class, () -> service.updateCourse(10L, dto, caller));
        assertTrue(ex.getMessage().toLowerCase().contains("not authorized"));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourse_removingLessonWithProgress_throwsIllegalStateException() {
        // arrange
        User instr = TestDataFactory.makeUser(3L, "instr", "instr@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        Course course = courseWithInstructorAndLesson(20L, instr, 300L);

        when(courseRepository.findById(20L)).thenReturn(Optional.of(course));

        // CourseUpdateDto that omits the existing lesson -> triggers removal attempt
        com.example.lms.dto.CourseUpdateDto dto = new com.example.lms.dto.CourseUpdateDto();
        dto.setTitle("updated");
        dto.setDescription("d");
        dto.setLessons(List.of()); // empty -> remove existing lesson

        // simulate progress exists for that lesson id
        when(lessonProgressRepository.existsByLessonId(300L)).thenReturn(true);

        // act / assert
        IllegalStateException ise = assertThrows(IllegalStateException.class,
                () -> service.updateCourse(20L, dto, instr));
        assertTrue(ise.getMessage().toLowerCase().contains("cannot remove lesson") ||
                ise.getMessage().toLowerCase().contains("already completed"));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void getEnrolledStudentsWithProgress_whenNotInstructor_throwsSecurityException() {
        // arrange
        User owner = TestDataFactory.makeUser(4L, "owner2", "owner2@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        User caller = TestDataFactory.makeUser(5L, "caller2", "caller2@x.com", com.example.lms.entity.Role.INSTRUCTOR);
        Course course = TestDataFactory.makeCourse(30L, "C30", "d", owner, false);

        when(courseRepository.findById(30L)).thenReturn(Optional.of(course));

        // act / assert
        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getEnrolledStudentsWithProgress(30L, caller));
        assertTrue(ex.getMessage().toLowerCase().contains("not authorized"));

        // ensure no repository call to enrollment when unauthorized
        verify(enrollmentRepository, never()).findByCourse(any());
    }

    @Test
    void updateCourse_whenRemovingLessonWithProgress_executesExistsByLessonIdBranch() throws Exception {
        // arrange
        User instr = TestDataFactory.makeUser(777L, "instr", "instr@x.com", com.example.lms.entity.Role.INSTRUCTOR);

        // create course and ensure lessons list exists
        Course course = TestDataFactory.makeCourse(999L, "C", "d", instr, false);
        try {
            course.setLessons(new ArrayList<>());
        } catch (Throwable ignored) {
        }
        // create lesson and force a non-null id (defensive: set via setter or
        // reflection)
        Lesson lesson = TestDataFactory.makeLesson(201L, "L-to-remove", course);

        // Defensive: ensure the lesson.id is actually set; some projects don't expose
        // setId
        if (lesson.getId() == null) {
            // reflection fallback to set private field "id"
            java.lang.reflect.Field f = lesson.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(lesson, 201L);
        }

        course.getLessons().add(lesson);

        when(courseRepository.findById(999L)).thenReturn(Optional.of(course));

        // Update DTO intentionally omits the existing lesson -> causes removal attempt
        com.example.lms.dto.CourseUpdateDto updateDto = new com.example.lms.dto.CourseUpdateDto();
        updateDto.setTitle("ignored");
        updateDto.setDescription("ignored");
        updateDto.setLessons(Collections.emptyList()); // <- indicates remove existing lessons

        // THIS IS THE CRUCIAL STUB: force existsByLessonId(...) to return true for
        // lesson id 201
        when(lessonProgressRepository.existsByLessonId(201L)).thenReturn(true);

        // act + assert: should throw because service checks existsByLessonId and then
        // throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateCourse(999L, updateDto, instr));

        // assert error message is the "cannot remove" branch (optional but helpful)
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        assertTrue(msg.contains("cannot remove") || msg.contains("completed") || msg.contains("already completed"));

        // verify that the existsByLessonId branch was actually invoked with the lesson
        // id
        verify(lessonProgressRepository, times(1)).existsByLessonId(201L);

        // and ensure we never saved the course (since operation aborted)
        verify(courseRepository, never()).save(any());
    }

}
