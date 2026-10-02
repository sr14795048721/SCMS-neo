package com.scms.core.homebanner.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.homebanner.domain.BannerScope;
import com.scms.core.homebanner.domain.HomeBannerMediaType;
import com.scms.core.homebanner.dto.PublicHomeBannersResponse;
import com.scms.core.homebanner.service.BannerMediaContent;
import com.scms.core.homebanner.service.HomeBannerService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/public/home-banners")
public class PublicHomeBannerController {

    private static final BannerScope SCOPE = BannerScope.HOME;

    private final HomeBannerService homeBannerService;
    private final ApiResponseFactory responseFactory;

    public PublicHomeBannerController(HomeBannerService homeBannerService, ApiResponseFactory responseFactory) {
        this.homeBannerService = homeBannerService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PublicHomeBannersResponse> list() {
        return responseFactory.success(new PublicHomeBannersResponse(homeBannerService.listPublic(SCOPE)));
    }

    @GetMapping("/{bannerId}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable("bannerId") Long bannerId,
                                                       @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        BannerMediaContent mediaContent = homeBannerService.resolveMainContent(SCOPE, bannerId);
        HomeBannerMediaType mediaType = homeBannerService.getMediaType(SCOPE, bannerId);
        if (mediaType == HomeBannerMediaType.VIDEO) {
            return buildVideoResponse(mediaContent, rangeHeader);
        }
        return buildBinaryResponse(mediaContent);
    }

    @GetMapping("/{bannerId}/poster")
    public ResponseEntity<InputStreamResource> poster(@PathVariable("bannerId") Long bannerId) {
        return buildBinaryResponse(homeBannerService.resolvePosterContent(SCOPE, bannerId));
    }

    static ResponseEntity<InputStreamResource> buildBinaryResponse(BannerMediaContent mediaContent) {
        try {
            InputStream inputStream = Files.newInputStream(mediaContent.path());
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.parseMediaType(mediaContent.contentType()))
                    .contentLength(mediaContent.contentLength())
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    static ResponseEntity<InputStreamResource> buildVideoResponse(BannerMediaContent mediaContent, String rangeHeader) {
        MediaType mediaType = MediaType.parseMediaType(mediaContent.contentType());
        try {
            if (rangeHeader == null || rangeHeader.isBlank()) {
                InputStream inputStream = Files.newInputStream(mediaContent.path());
                return ResponseEntity.ok()
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentType(mediaType)
                        .contentLength(mediaContent.contentLength())
                        .body(new InputStreamResource(inputStream));
            }

            ByteRange byteRange = parseRange(rangeHeader, mediaContent.contentLength());
            if (byteRange == null) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + mediaContent.contentLength())
                        .build();
            }

            InputStream inputStream = Files.newInputStream(mediaContent.path());
            inputStream.skipNBytes(byteRange.start());
            InputStreamResource body = new InputStreamResource(new LimitedInputStream(inputStream, byteRange.length()));

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + byteRange.start() + "-" + byteRange.end() + "/" + mediaContent.contentLength())
                    .contentType(mediaType)
                    .contentLength(byteRange.length())
                    .body(body);
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static ByteRange parseRange(String header, long contentLength) {
        if (!header.startsWith("bytes=")) {
            return null;
        }

        String rangeValue = header.substring("bytes=".length()).trim();
        int commaIndex = rangeValue.indexOf(',');
        if (commaIndex >= 0) {
            rangeValue = rangeValue.substring(0, commaIndex).trim();
        }
        int dashIndex = rangeValue.indexOf('-');
        if (dashIndex < 0) {
            return null;
        }

        String startValue = rangeValue.substring(0, dashIndex).trim();
        String endValue = rangeValue.substring(dashIndex + 1).trim();
        try {
            long start;
            long end;
            if (startValue.isEmpty()) {
                long suffixLength = Long.parseLong(endValue);
                if (suffixLength <= 0) {
                    return null;
                }
                start = Math.max(contentLength - suffixLength, 0);
                end = contentLength - 1;
            } else {
                start = Long.parseLong(startValue);
                end = endValue.isEmpty() ? contentLength - 1 : Long.parseLong(endValue);
            }

            if (start < 0 || end < start || start >= contentLength) {
                return null;
            }
            end = Math.min(end, contentLength - 1);
            return new ByteRange(start, end);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record ByteRange(long start, long end) {
        long length() {
            return end - start + 1;
        }
    }

    private static final class LimitedInputStream extends FilterInputStream {

        private long remaining;

        private LimitedInputStream(InputStream inputStream, long remaining) {
            super(inputStream);
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int maxLength = (int) Math.min(length, remaining);
            int read = super.read(buffer, offset, maxLength);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
