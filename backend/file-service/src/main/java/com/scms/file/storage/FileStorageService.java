package com.scms.file.storage;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private volatile boolean bucketReady;

    public FileStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String upload(MultipartFile file) {
        try {
            ensureBucketExists();
            String objectKey = buildObjectKey(file.getOriginalFilename());
            try (InputStream stream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectKey)
                                .stream(stream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("upload file failed", exception);
        }
    }

    public InputStream download(String objectKey) {
        try {
            ensureBucketExists();
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("download file failed", exception);
        }
    }

    public String presignedDownloadUrl(String objectKey, int expirySeconds) {
        try {
            ensureBucketExists();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("generate download url failed", exception);
        }
    }

    private String buildObjectKey(String originalFilename) {
        String safeName = originalFilename == null ? "file.bin" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return LocalDate.now() + "-" + UUID.randomUUID() + "_" + safeName;
    }

    private void ensureBucketExists() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
        }
        bucketReady = true;
    }
}
