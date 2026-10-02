package com.scms.core.manager.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.config.ManagerAvatarStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ManagerAvatarStorageService {

    private static final long IMAGE_MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final ManagerAvatarStorageProperties storageProperties;

    public ManagerAvatarStorageService(ManagerAvatarStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredManagerAvatar store(MultipartFile file) {
        validateFile(file);
        String normalizedContentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(normalizedContentType, file.getOriginalFilename());
        String relativePath = UUID.randomUUID() + extension;
        Path absolutePath = resolveAbsolutePath(relativePath);

        ensureParentDirectory(absolutePath);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, absolutePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "store manager avatar failed");
        }

        return new StoredManagerAvatar(relativePath);
    }

    public ManagerAvatarContent resolveContent(String relativePath) {
        Path absolutePath = resolveAbsolutePath(relativePath);
        if (!Files.exists(absolutePath) || !Files.isRegularFile(absolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "manager avatar not found");
        }

        try {
            return new ManagerAvatarContent(
                    absolutePath,
                    resolveContentType(absolutePath),
                    Files.size(absolutePath)
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "read manager avatar failed");
        }
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveAbsolutePath(relativePath));
        } catch (IOException ignored) {
            // Ignore cleanup failures to keep the main flow resilient.
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "avatar file is required");
        }
        if (file.getSize() > IMAGE_MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "avatar file exceeds size limit");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!IMAGE_EXTENSIONS.containsKey(contentType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "avatar file type is not supported");
        }
    }

    private String resolveExtension(String contentType, String originalFilename) {
        String byType = IMAGE_EXTENSIONS.get(contentType);
        if (byType != null) {
            return byType;
        }

        String filename = String.valueOf(originalFilename == null ? "" : originalFilename).trim().toLowerCase(Locale.ROOT);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = filename.substring(dotIndex);
            if (IMAGE_EXTENSIONS.containsValue(extension)) {
                return extension;
            }
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "avatar file extension is not supported");
    }

    private Path resolveAbsolutePath(String relativePath) {
        Path root = Paths.get(storageProperties.getManagerAvatarRoot()).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "invalid manager avatar storage path");
        }
        return resolved;
    }

    private void ensureParentDirectory(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "create manager avatar storage directory failed");
        }
    }

    private String resolveContentType(Path path) throws IOException {
        String probed = Files.probeContentType(path);
        if (probed != null && !probed.isBlank()) {
            return probed;
        }

        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String normalizeContentType(String contentType) {
        return String.valueOf(contentType == null ? "" : contentType)
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
