package com.scms.core.clubappworkspace.service;

import com.scms.core.clubappworkspace.config.AppWorkspaceProperties;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;

import java.net.URLConnection;

@Service
public class ClubAppWorkspaceObjectStorageService {

    private final AppWorkspaceProperties properties;
    private volatile MinioClient minioClient;

    public ClubAppWorkspaceObjectStorageService(AppWorkspaceProperties properties) {
        this.properties = properties;
    }

    public ClubAppWorkspaceMaterialContent download(String storagePath) {
        AppWorkspaceProperties.Storage storage = properties.getStorage();
        if (!storage.isConfigured()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "workspace material storage is not configured");
        }

        String objectKey = normalizeObjectKey(storagePath);
        try {
            MinioClient client = getOrCreateClient(storage);
            StatObjectResponse objectStat = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(storage.getBucket())
                            .object(objectKey)
                            .build()
            );
            GetObjectResponse objectStream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(storage.getBucket())
                            .object(objectKey)
                            .build()
            );
            return new ClubAppWorkspaceMaterialContent(
                    objectStream,
                    resolveContentType(objectStat.contentType(), objectKey),
                    objectStat.size(),
                    resolveFileName(objectKey)
            );
        } catch (ErrorResponseException exception) {
            String code = exception.errorResponse() == null ? "" : exception.errorResponse().code();
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchObject".equalsIgnoreCase(code)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "workspace material file not found");
            }
            throw new IllegalStateException("download workspace material failed", exception);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("download workspace material failed", exception);
        }
    }

    private MinioClient getOrCreateClient(AppWorkspaceProperties.Storage storage) {
        MinioClient current = minioClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (minioClient == null) {
                minioClient = MinioClient.builder()
                        .endpoint(storage.getEndpoint())
                        .credentials(storage.getAccessKey(), storage.getSecretKey())
                        .build();
            }
            return minioClient;
        }
    }

    private String normalizeObjectKey(String storagePath) {
        String normalized = storagePath == null ? "" : storagePath.trim().replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace material file not found");
        }
        return normalized;
    }

    private String resolveContentType(String contentType, String objectKey) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String guessed = URLConnection.guessContentTypeFromName(objectKey);
        return guessed == null || guessed.isBlank() ? "application/octet-stream" : guessed;
    }

    private String resolveFileName(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }
}
