package com.scms.core.news.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.news.dto.AdminNewsDetailResponse;
import com.scms.core.news.dto.AdminNewsPageResponse;
import com.scms.core.news.dto.UpdateNewsRequest;
import com.scms.core.news.dto.UploadedNewsAssetResponse;
import com.scms.core.news.service.NewsMediaContent;
import com.scms.core.news.service.NewsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/admin/news")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminNewsController {

    private final NewsService newsService;
    private final ApiResponseFactory responseFactory;

    public AdminNewsController(NewsService newsService, ApiResponseFactory responseFactory) {
        this.newsService = newsService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminNewsPageResponse> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                                   @RequestParam(name = "source", defaultValue = "") String source) {
        return responseFactory.success(newsService.listAdmin(page, pageSize, source));
    }

    @PostMapping
    public ApiResponse<AdminNewsDetailResponse> create() {
        return responseFactory.success(newsService.createDraft());
    }

    @GetMapping("/{newsId}")
    public ApiResponse<AdminNewsDetailResponse> detail(@PathVariable("newsId") Long newsId) {
        return responseFactory.success(newsService.getAdminDetail(newsId));
    }

    @PatchMapping("/{newsId}")
    public ApiResponse<AdminNewsDetailResponse> update(@PathVariable("newsId") Long newsId,
                                                       @RequestBody UpdateNewsRequest request) {
        return responseFactory.success(newsService.update(newsId, request));
    }

    @PostMapping(path = "/{newsId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminNewsDetailResponse> uploadCover(@PathVariable("newsId") Long newsId,
                                                            @RequestPart("file") MultipartFile file) {
        return responseFactory.success(newsService.uploadCover(newsId, file));
    }

    @DeleteMapping("/{newsId}/cover")
    public ApiResponse<AdminNewsDetailResponse> deleteCover(@PathVariable("newsId") Long newsId) {
        return responseFactory.success(newsService.deleteCover(newsId));
    }

    @GetMapping("/{newsId}/cover/content")
    public ResponseEntity<InputStreamResource> coverContent(@PathVariable("newsId") Long newsId) {
        return buildBinaryResponse(newsService.resolveAdminCoverContent(newsId));
    }

    @PostMapping(path = "/{newsId}/assets/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedNewsAssetResponse> uploadBodyImage(@PathVariable("newsId") Long newsId,
                                                                  @RequestPart("file") MultipartFile file) {
        return responseFactory.success(newsService.uploadBodyImage(newsId, file));
    }

    @GetMapping("/{newsId}/assets/{assetId}/content")
    public ResponseEntity<InputStreamResource> assetContent(@PathVariable("newsId") Long newsId,
                                                            @PathVariable("assetId") Long assetId) {
        return buildBinaryResponse(newsService.resolveAdminAssetContent(newsId, assetId));
    }

    @DeleteMapping("/{newsId}")
    public ApiResponse<Void> delete(@PathVariable("newsId") Long newsId) {
        newsService.delete(newsId);
        return responseFactory.success(null);
    }

    private ResponseEntity<InputStreamResource> buildBinaryResponse(NewsMediaContent mediaContent) {
        try {
            InputStream inputStream = Files.newInputStream(mediaContent.path());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(mediaContent.contentType()))
                    .contentLength(mediaContent.contentLength())
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
