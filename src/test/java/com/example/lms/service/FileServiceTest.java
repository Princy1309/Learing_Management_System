package com.example.lms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class FileServiceTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        s3Client = Mockito.mock(S3Client.class);
        s3Presigner = Mockito.mock(S3Presigner.class);
        fileService = new FileService(s3Client, s3Presigner);
    }

    @Test
    void uploadFile_throwsException_whenBucketNameNotConfigured() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        ReflectionTestUtils.setField(fileService, "bucketName", ""); // simulate missing config

        assertThrows(IllegalStateException.class, () -> fileService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_uploadsAndReturnsPresignedUrl() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getBytes()).thenReturn("hello".getBytes());
        when(mockFile.getContentType()).thenReturn("text/plain");

        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");

        // mock presigner URL response
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/file.txt"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = fileService.uploadFile(mockFile);

        assertNotNull(url);
        assertTrue(url.contains("https://example.com"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void generatePresignedUrl_returnsValidUrl() {
        ReflectionTestUtils.setField(fileService, "bucketName", "demo-bucket");

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(mock(URL.class));
        when(presigned.url().toString()).thenReturn("https://signed.url");

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = fileService.generatePresignedUrl("fileKey", 30);

        assertEquals("https://signed.url", url);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void uploadFile_callsS3PutObject_andReturnsUrl() throws IOException {
        S3Client s3Client = Mockito.mock(S3Client.class);
        S3Presigner presigner = Mockito.mock(S3Presigner.class);

        FileService fileService = new FileService(s3Client, presigner);

        // inject properties via reflection for test (or set in a test config)
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "bucketName", "my-bucket");
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "region", "ap-south-1");

        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        FileService spyService = Mockito.spy(fileService);
        Mockito.doReturn("https://example.com/hello").when(spyService).generatePresignedUrl(any(), anyLong());

        String url = spyService.uploadFile(file);
        assertNotNull(url);
        assertTrue(url.startsWith("http"));

        // verify S3 put was invoked
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_usesDefaultFilename_whenOriginalIsNull() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(null); // triggers the "file" default
        when(mockFile.getBytes()).thenReturn("abc".getBytes());
        when(mockFile.getContentType()).thenReturn("text/plain");

        ReflectionTestUtils.setField(fileService, "bucketName", "bucket");

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/file.txt"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = fileService.uploadFile(mockFile);

        assertNotNull(url);
        assertTrue(url.startsWith("https://"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

}
