package com.scms.core.homebanner;

import com.scms.core.homebanner.config.BannerStorageProperties;
import com.scms.core.homebanner.config.MediaProcessingProperties;
import com.scms.core.homebanner.domain.HomeBannerMediaType;
import com.scms.core.homebanner.service.BannerMediaStorageService;
import com.scms.core.homebanner.service.StoredBannerMedia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannerMediaStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeShouldFallbackToImageIoWhenFfmpegIsUnavailable() throws IOException {
        BannerMediaStorageService service = createService();
        byte[] sourceBytes = createJpegBytes(2200, 1400);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                sourceBytes
        );

        StoredBannerMedia stored = service.store(HomeBannerMediaType.IMAGE, file);

        Path storedPath = tempDir.resolve(stored.mediaPath());
        assertTrue(Files.exists(storedPath));
        assertEquals("image/jpeg", stored.mediaContentType());
        assertEquals(3, stored.mediaOptimizedVersion());
        assertTrue(stored.mediaSizeBytes() > 0);

        BufferedImage image = ImageIO.read(storedPath.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() <= 320);
        assertTrue(image.getHeight() <= 180);
    }

    @Test
    void recompressImageShouldFallbackToImageIoWhenFfmpegIsUnavailable() throws IOException {
        BannerMediaStorageService service = createService();
        Path sourcePath = tempDir.resolve("images/existing.jpg");
        Files.createDirectories(sourcePath.getParent());
        Files.write(sourcePath, createJpegBytes(2000, 1200));

        StoredBannerMedia stored = service.recompressImage("images/existing.jpg", "image/jpeg");

        assertEquals("images/existing.jpg", stored.mediaPath());
        assertEquals("image/jpeg", stored.mediaContentType());
        assertEquals(3, stored.mediaOptimizedVersion());

        BufferedImage image = ImageIO.read(sourcePath.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() <= 320);
        assertTrue(image.getHeight() <= 180);
    }

    private BannerMediaStorageService createService() {
        BannerStorageProperties storageProperties = new BannerStorageProperties();
        storageProperties.setBannerRoot(tempDir.toString());

        MediaProcessingProperties mediaProcessingProperties = new MediaProcessingProperties();
        mediaProcessingProperties.setFfmpegPath("ffmpeg-command-that-does-not-exist");
        mediaProcessingProperties.setBannerImageMaxWidth(320);
        mediaProcessingProperties.setBannerImageMaxHeight(180);
        mediaProcessingProperties.setBannerJpegQuality(80);
        mediaProcessingProperties.setBannerOptimizationVersion(3);

        return new BannerMediaStorageService(storageProperties, mediaProcessingProperties);
    }

    private byte[] createJpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(94, 59, 171));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(250, 250, 255));
            graphics.fillRoundRect(80, 80, width - 160, height - 160, 48, 48);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }
}
