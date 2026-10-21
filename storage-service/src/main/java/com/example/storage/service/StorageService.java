package com.example.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Service
public class StorageService {

    private final S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucketName;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String folder, MultipartFile file) throws IOException {
        String key = folder + "/" + file.getOriginalFilename();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    }

    public byte[] downloadFile(String folder, String filename) {
        String key = folder + "/" + filename;
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void deleteFile(String folder, String filename) {
        String key = folder + "/" + filename;
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    public boolean hasAccessToDepartment(String userEmail, String department) {
        return userEmail.equals("admin@example.com") || department.equals("engineering") || department.equals("hr");
    }
}
