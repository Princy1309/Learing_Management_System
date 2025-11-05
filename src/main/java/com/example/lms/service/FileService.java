package com.example.lms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    // inject both S3Client and S3Presigner (configured in S3Config)
    public FileService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("S3 bucket name not configured. Set aws.s3.bucket-name.");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "_" + original.replaceAll("\\s+", "_");

        // Build PutObjectRequest without ACL (do not set .acl(...))
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));

        // Return a presigned URL for the uploaded object (valid for 60 minutes)
        return generatePresignedUrl(fileName, 60);
    }

    /**
     * Generate presigned GET URL for the given object key (expiresInMinutes
     * minutes).
     */
    public String generatePresignedUrl(String objectKey, long expiresInMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(expiresInMinutes))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
