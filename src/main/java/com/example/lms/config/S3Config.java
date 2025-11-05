
package com.example.lms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private final String accessKey;
    private final String secretKey;
    private final String region;

    public S3Config(
            @Value("${aws.s3.access-key:}") String accessKey,
            @Value("${aws.s3.secret-key:}") String secretKey,
            @Value("${aws.s3.region:}") String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    @Bean
    public S3Client s3Client() {
        // Validate configuration early with a clear message
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("AWS credentials are not configured. " +
                    "Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY as environment variables " +
                    "or put aws.s3.access-key / aws.s3.secret-key in application.properties.");
        }
        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("AWS region is not configured. Set AWS_REGION or " +
                    "aws.s3.region in application.properties.");
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        // Use same credentials & region as S3Client
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }
}
