package com.example.lms.controller;

import com.example.lms.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileControllerTest {

    @Mock
    private FileService fileService;
    @InjectMocks
    private FileController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadFile_success_returnsOkWithUrl() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Principal principal = () -> "user@x.com";

        when(fileService.uploadFile(file)).thenReturn("http://cdn.example.com/file123");

        ResponseEntity<?> resp = controller.uploadFile(file, principal);

        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().contains("File uploaded successfully"));
        assertTrue(resp.getBody().toString().contains("http://cdn.example.com/file123"));

        verify(fileService).uploadFile(file);
    }

    @Test
    void uploadFile_failure_returns400WithMessage() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Principal principal = null; // anonymous caller path

        when(fileService.uploadFile(file)).thenThrow(new RuntimeException("disk full"));

        ResponseEntity<?> resp = controller.uploadFile(file, principal);

        assertEquals(400, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().toLowerCase().contains("upload failed"));
        assertTrue(resp.getBody().toString().toLowerCase().contains("disk full"));

        verify(fileService).uploadFile(file);
    }
}
