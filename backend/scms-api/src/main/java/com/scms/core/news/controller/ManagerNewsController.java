package com.scms.core.news.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.news.dto.AdminNewsDetailResponse;
import com.scms.core.news.dto.AdminNewsPageResponse;
import com.scms.core.news.dto.UpdateNewsRequest;
import com.scms.core.news.dto.UploadedNewsAssetResponse;
import com.scms.core.news.service.NewsMediaContent;
import com.scms.core.news.service.NewsService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/managers/me/news")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerNewsController {

    private final NewsService newsService;
    private final ApiResponseFactory responseFactory;

    public ManagerNewsController(NewsService newsService, ApiResponseFactory responseFactory) {
        this.newsService = newsService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminNewsPageResponse> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return responseFactory.success(newsService.listManager(page, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminNewsDetailResponse> create() {
        return responseFactory.success(newsService.createManagerDraft());
    }

    @GetMapping("/{newsId}")
    public ApiResponse<AdminNewsDetailResponse> detail(@PathVariable("newsId") Long newsId) {
        return responseFactory.success(newsService.getManagerDetail(newsId));
    }

    @PatchMapping("/{newsId}")
    public ApiResponse<AdminNewsDetailResponse> update(@PathVariable("newsId") Long newsId,
                                                       @Valid @RequestBody UpdateNewsRequest request) {
        return responseFactory.success(newsService.updateManagerDraft(newsId, request));
    }

    @DeleteMapping("/{newsId}")
    public ApiResponse<Void> delete(@PathVariable("newsId") Long newsId) {
        newsService.deleteManagerDraft(newsId);
        return responseFactory.success(null);
    }

    @PostMapping(path = "/{newsId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminNewsDetailResponse> uploadCover(@PathVariable("newsId") Long newsId,
                                                            @RequestPart("file") MultipartFile file) {
        return responseFactory.success(newsService.uploadManagerCover(newsId, file));
    }

    @DeleteMapping("/{newsId}/cover")
    public ApiResponse<AdminNewsDetailResponse> deleteCover(@PathVariable("newsId") Long newsId) {
        return responseFactory.success(newsService.deleteManagerCover(newsId));
    }

    @GetMapping("/{newsId}/cover/content")
    public ResponseEntity<InputStreamResource> coverContent(@PathVariable("newsId") Long newsId) {
        return buildBinaryResponse(newsService.resolveManagerCoverContent(newsId));
    }

    @PostMapping(path = "/{newsId}/assets/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedNewsAssetResponse> uploadBodyImage(@PathVariable("newsId") Long newsId,
                                                                  @RequestPart("file") MultipartFile file) {
        return responseFactory.success(newsService.uploadManagerBodyImage(newsId, file));
    }

    @GetMapping("/{newsId}/assets/{assetId}/content")
    public ResponseEntity<InputStreamResource> assetContent(@PathVariable("newsId") Long newsId,
                                                            @PathVariable("assetId") Long assetId) {
        return buildBinaryResponse(newsService.resolveManagerAssetContent(newsId, assetId));
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
