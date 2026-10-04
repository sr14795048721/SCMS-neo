package com.scms.core.attendance.service;

import com.scms.core.attendance.config.AttendanceSignatureStorageProperties;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * Stores handwritten attendance signature images (base64 data URLs) on local disk.
 */
@Service
public class AttendanceSignatureStorageService {

    private static final String DATA_URL_PREFIX = "data:image/png;base64,";
    private static final int MAX_BASE64_LENGTH = 2_800_000;

    private final AttendanceSignatureStorageProperties storageProperties;

    public AttendanceSignatureStorageService(AttendanceSignatureStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String storeSignature(String signatureDataUrl) {
        String normalized = signatureDataUrl == null ? "" : signatureDataUrl.trim();
        if (!normalized.startsWith(DATA_URL_PREFIX)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "signature must be a png data url");
        }
        String base64 = normalized.substring(DATA_URL_PREFIX.length());
        if (base64.isEmpty() || base64.length() > MAX_BASE64_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "signature size is invalid");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "signature is not valid base64");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "signature image is empty");
        }

        String relativePath = UUID.randomUUID() + ".png";
        Path absolutePath = resolveAbsolutePath(relativePath);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, bytes);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "store attendance signature failed");
        }
        return relativePath;
    }

    public Path resolveContent(String relativePath) {
        Path absolutePath = resolveAbsolutePath(relativePath);
        if (!Files.exists(absolutePath) || !Files.isRegularFile(absolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "attendance signature not found");
        }
        return absolutePath;
    }

    private Path resolveAbsolutePath(String relativePath) {
        Path root = Paths.get(storageProperties.getAttendanceSignatureRoot()).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "invalid attendance signature storage path");
        }
        return resolved;
    }
}
