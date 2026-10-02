package com.scms.core.homebanner.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homebanner.config.BannerStorageProperties;
import com.scms.core.homebanner.config.MediaProcessingProperties;
import com.scms.core.homebanner.domain.HomeBannerMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class BannerMediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(BannerMediaStorageService.class);
    private static final long IMAGE_MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE_BYTES = 200L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );
    private static final Map<String, String> VIDEO_EXTENSIONS = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/ogg", ".ogg"
    );
    private static final String POSTER_EXTENSION = ".jpg";
    private static final String POSTER_CONTENT_TYPE = "image/jpeg";

    private final BannerStorageProperties storageProperties;
    private final MediaProcessingProperties mediaProcessingProperties;

    public BannerMediaStorageService(BannerStorageProperties storageProperties,
                                     MediaProcessingProperties mediaProcessingProperties) {
        this.storageProperties = storageProperties;
        this.mediaProcessingProperties = mediaProcessingProperties;
    }

    public StoredBannerMedia store(HomeBannerMediaType mediaType, MultipartFile file) {
        validateFile(mediaType, file);

        String normalizedContentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(mediaType, normalizedContentType, file.getOriginalFilename());
        String mediaRelativePath = resolveMediaRelativePath(mediaType, extension);
        Path mediaAbsolutePath = resolveAbsolutePath(mediaRelativePath);
        ensureParentDirectory(mediaAbsolutePath);

        if (mediaType == HomeBannerMediaType.IMAGE) {
            return storeOptimizedImage(file, normalizedContentType, extension, mediaRelativePath, mediaAbsolutePath);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, mediaAbsolutePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "store banner media failed");
        }

        String posterRelativePath = "posters/" + UUID.randomUUID() + POSTER_EXTENSION;
        Path posterAbsolutePath = resolveAbsolutePath(posterRelativePath);
        ensureParentDirectory(posterAbsolutePath);

        try {
            extractPoster(mediaAbsolutePath, posterAbsolutePath);
        } catch (RuntimeException exception) {
            deleteIfExists(mediaRelativePath);
            deleteIfExists(posterRelativePath);
            throw exception;
        }

        return new StoredBannerMedia(
                mediaRelativePath,
                normalizedContentType,
                file.getSize(),
                null,
                posterRelativePath,
                POSTER_CONTENT_TYPE
        );
    }

    public StoredBannerMedia recompressImage(String relativePath, String contentType) {
        String normalizedContentType = resolveImageContentType(relativePath, contentType);
        Path sourceAbsolutePath = resolveAbsolutePath(relativePath);
        if (!Files.exists(sourceAbsolutePath) || !Files.isRegularFile(sourceAbsolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "banner media not found");
        }

        Path tempOutputPath = createSiblingTempPath(sourceAbsolutePath, ".optimized");
        try {
            optimizeImage(sourceAbsolutePath, tempOutputPath, normalizedContentType);
            Files.move(tempOutputPath, sourceAbsolutePath, StandardCopyOption.REPLACE_EXISTING);
            long optimizedSize = Files.size(sourceAbsolutePath);
            return new StoredBannerMedia(
                    relativePath,
                    normalizedContentType,
                    optimizedSize,
                    getBannerOptimizationVersion(),
                    null,
                    null
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "replace optimized banner media failed");
        } finally {
            deleteAbsolutePathIfExists(tempOutputPath);
        }
    }

    public BannerMediaContent resolveContent(String relativePath, String contentType) {
        Path absolutePath = resolveAbsolutePath(relativePath);
        if (!Files.exists(absolutePath) || !Files.isRegularFile(absolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "banner media not found");
        }
        try {
            return new BannerMediaContent(absolutePath, contentType, Files.size(absolutePath));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "read banner media failed");
        }
    }

    public int getBannerOptimizationVersion() {
        return Math.max(1, mediaProcessingProperties.getBannerOptimizationVersion());
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveAbsolutePath(relativePath));
        } catch (IOException ignored) {
            // Ignore file cleanup failures to keep delete and replace flows resilient.
        }
    }

    private StoredBannerMedia storeOptimizedImage(MultipartFile file,
                                                  String normalizedContentType,
                                                  String extension,
                                                  String mediaRelativePath,
                                                  Path mediaAbsolutePath) {
        Path sourceAbsolutePath = createWorkingFile(mediaAbsolutePath.getParent(), "banner-src-", extension);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, sourceAbsolutePath, StandardCopyOption.REPLACE_EXISTING);
            optimizeImage(sourceAbsolutePath, mediaAbsolutePath, normalizedContentType);
            long optimizedSize = Files.size(mediaAbsolutePath);
            return new StoredBannerMedia(
                    mediaRelativePath,
                    normalizedContentType,
                    optimizedSize,
                    getBannerOptimizationVersion(),
                    null,
                    null
            );
        } catch (IOException exception) {
            deleteAbsolutePathIfExists(mediaAbsolutePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "store banner media failed");
        } catch (RuntimeException exception) {
            deleteAbsolutePathIfExists(mediaAbsolutePath);
            throw exception;
        } finally {
            deleteAbsolutePathIfExists(sourceAbsolutePath);
        }
    }

    private void validateFile(HomeBannerMediaType mediaType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file is required");
        }

        long maxSize = mediaType == HomeBannerMediaType.IMAGE ? IMAGE_MAX_SIZE_BYTES : VIDEO_MAX_SIZE_BYTES;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file exceeds size limit");
        }

        String contentType = normalizeContentType(file.getContentType());
        boolean supported = mediaType == HomeBannerMediaType.IMAGE
                ? IMAGE_EXTENSIONS.containsKey(contentType)
                : VIDEO_EXTENSIONS.containsKey(contentType);
        if (!supported) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file type is not supported");
        }
    }

    private String resolveExtension(HomeBannerMediaType mediaType, String contentType, String originalFilename) {
        Map<String, String> contentTypeMap = mediaType == HomeBannerMediaType.IMAGE ? IMAGE_EXTENSIONS : VIDEO_EXTENSIONS;
        String byType = contentTypeMap.get(contentType);
        if (byType != null) {
            return byType;
        }

        String filename = String.valueOf(originalFilename == null ? "" : originalFilename).trim().toLowerCase(Locale.ROOT);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = filename.substring(dotIndex);
            if (contentTypeMap.containsValue(extension)) {
                return extension;
            }
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file extension is not supported");
    }

    private String resolveMediaRelativePath(HomeBannerMediaType mediaType, String extension) {
        String directory = mediaType == HomeBannerMediaType.IMAGE ? "images/" : "videos/";
        return directory + UUID.randomUUID() + extension;
    }

    private Path resolveAbsolutePath(String relativePath) {
        Path root = Paths.get(storageProperties.getBannerRoot()).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "invalid banner storage path");
        }
        return resolved;
    }

    private void ensureParentDirectory(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "create banner storage directory failed");
        }
    }

    private String normalizeContentType(String contentType) {
        return String.valueOf(contentType == null ? "" : contentType)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String resolveImageContentType(String relativePath, String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        if (IMAGE_EXTENSIONS.containsKey(normalizedContentType)) {
            return normalizedContentType;
        }

        String loweredPath = String.valueOf(relativePath == null ? "" : relativePath).trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : IMAGE_EXTENSIONS.entrySet()) {
            if (loweredPath.endsWith(entry.getValue())) {
                return entry.getKey();
            }
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file type is not supported");
    }

    private Path createWorkingFile(Path directory, String prefix, String suffix) {
        try {
            return Files.createTempFile(directory, prefix, suffix);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "create banner temp file failed");
        }
    }

    private Path createSiblingTempPath(Path sourcePath, String marker) {
        String fileName = sourcePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String name = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex) : "";
        return sourcePath.resolveSibling(name + marker + extension);
    }

    private void optimizeImage(Path sourcePath, Path outputPath, String contentType) {
        try {
            optimizeImageWithFfmpeg(sourcePath, outputPath, contentType);
            return;
        } catch (RuntimeException exception) {
            if (!supportsJavaImageFallback(contentType)) {
                throw exception;
            }
            log.warn("ffmpeg banner optimization failed, fallback to ImageIO for {}", sourcePath, exception);
        }

        optimizeImageWithImageIo(sourcePath, outputPath, contentType);
    }

    private void optimizeImageWithFfmpeg(Path sourcePath, Path outputPath, String contentType) {
        List<String> command = new ArrayList<>();
        command.add(mediaProcessingProperties.getFfmpegPath());
        command.add("-y");
        command.add("-i");
        command.add(sourcePath.toString());
        command.add("-vf");
        command.add("scale=" + normalizedWidth() + ":" + normalizedHeight() + ":force_original_aspect_ratio=decrease");
        command.add("-frames:v");
        command.add("1");

        switch (contentType) {
            case "image/jpeg" -> command.addAll(List.of(
                    "-q:v",
                    String.valueOf(mapJpegQualityToQValue(normalizedJpegQuality()))
            ));
            case "image/png" -> command.addAll(List.of(
                    "-pix_fmt",
                    "rgba",
                    "-compression_level",
                    "9",
                    "-pred",
                    "mixed"
            ));
            case "image/webp" -> command.addAll(List.of(
                    "-quality",
                    String.valueOf(normalizedWebpQuality()),
                    "-compression_level",
                    "6"
            ));
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner file type is not supported");
        }

        command.add(outputPath.toString());
        runExternalCommand(command, outputPath, "optimize banner image failed");
    }

    private boolean supportsJavaImageFallback(String contentType) {
        return "image/jpeg".equals(contentType) || "image/png".equals(contentType);
    }

    private void optimizeImageWithImageIo(Path sourcePath, Path outputPath, String contentType) {
        try {
            BufferedImage sourceImage = ImageIO.read(sourcePath.toFile());
            if (sourceImage == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "optimize banner image failed: unsupported image");
            }

            int sourceWidth = Math.max(1, sourceImage.getWidth());
            int sourceHeight = Math.max(1, sourceImage.getHeight());
            double scale = Math.min(
                    1D,
                    Math.min(
                            normalizedWidth() / (double) sourceWidth,
                            normalizedHeight() / (double) sourceHeight
                    )
            );
            int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

            boolean preserveAlpha = "image/png".equals(contentType);
            int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, imageType);

            Graphics2D graphics = outputImage.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                if (!preserveAlpha) {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, targetWidth, targetHeight);
                }
                graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }

            if ("image/jpeg".equals(contentType)) {
                writeJpeg(outputImage, outputPath);
                return;
            }

            if (!ImageIO.write(outputImage, "png", outputPath.toFile())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "optimize banner image failed: no PNG writer");
            }
        } catch (IOException exception) {
            String detail = exception.getMessage();
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    detail == null || detail.isBlank()
                            ? "optimize banner image failed"
                            : "optimize banner image failed: " + detail
            );
        }
    }

    private void writeJpeg(BufferedImage image, Path outputPath) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "optimize banner image failed: no JPEG writer");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputPath.toFile())) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(normalizedJpegQuality() / 100f);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private int normalizedWidth() {
        return Math.max(1, mediaProcessingProperties.getBannerImageMaxWidth());
    }

    private int normalizedHeight() {
        return Math.max(1, mediaProcessingProperties.getBannerImageMaxHeight());
    }

    private int normalizedJpegQuality() {
        return clamp(mediaProcessingProperties.getBannerJpegQuality(), 1, 100);
    }

    private int normalizedWebpQuality() {
        return clamp(mediaProcessingProperties.getBannerWebpQuality(), 1, 100);
    }

    private int mapJpegQualityToQValue(int jpegQuality) {
        int mapped = Math.round((101 - jpegQuality) / 3.5f);
        return clamp(mapped, 2, 31);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void extractPoster(Path sourceVideoPath, Path posterPath) {
        List<String> command = List.of(
                mediaProcessingProperties.getFfmpegPath(),
                "-y",
                "-ss",
                "00:00:00.500",
                "-i",
                sourceVideoPath.toString(),
                "-frames:v",
                "1",
                posterPath.toString()
        );
        runExternalCommand(command, posterPath, "extract banner poster failed");
    }

    private void runExternalCommand(List<String> command, Path expectedOutputPath, String errorMessage) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = readProcessOutput(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(expectedOutputPath)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage + ": " + output);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage + " interrupted");
        } catch (IOException exception) {
            String detail = exception.getMessage();
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    detail == null || detail.isBlank() ? errorMessage : errorMessage + ": " + detail
            );
        }
    }

    private void deleteAbsolutePathIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Ignore best-effort cleanup failures.
        }
    }

    private String readProcessOutput(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(line.trim());
            }
            return builder.toString();
        }
    }
}
