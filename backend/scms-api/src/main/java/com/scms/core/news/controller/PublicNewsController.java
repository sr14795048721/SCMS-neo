package com.scms.core.news.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.news.dto.PublicNewsDetailResponse;
import com.scms.core.news.dto.PublicNewsHomeResponse;
import com.scms.core.news.dto.PublicNewsPageResponse;
import com.scms.core.news.service.NewsMediaContent;
import com.scms.core.news.service.NewsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/public/news")
public class PublicNewsController {

    private final NewsService newsService;
    private final ApiResponseFactory responseFactory;

    public PublicNewsController(NewsService newsService, ApiResponseFactory responseFactory) {
        this.newsService = newsService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/home")
    public ApiResponse<PublicNewsHomeResponse> home() {
        return responseFactory.success(newsService.home());
    }

    @GetMapping
    public ApiResponse<PublicNewsPageResponse> list(@RequestParam(name = "page", defaultValue = "1") int page,
                                                    @RequestParam(name = "pageSize", defaultValue = "9") int pageSize) {
        return responseFactory.success(newsService.listPublic(page, pageSize));
    }

    @GetMapping("/{newsId}")
    public ApiResponse<PublicNewsDetailResponse> detail(@PathVariable("newsId") Long newsId) {
        return responseFactory.success(newsService.getPublicDetail(newsId));
    }

    @GetMapping("/{newsId}/cover")
    public ResponseEntity<InputStreamResource> cover(@PathVariable("newsId") Long newsId) {
        return buildBinaryResponse(newsService.resolveCoverContent(newsId));
    }

    @GetMapping("/{newsId}/assets/{assetId}")
    public ResponseEntity<InputStreamResource> asset(@PathVariable("newsId") Long newsId,
                                                     @PathVariable("assetId") Long assetId) {
        return buildBinaryResponse(newsService.resolveAssetContent(newsId, assetId));
    }

    private ResponseEntity<InputStreamResource> buildBinaryResponse(NewsMediaContent mediaContent) {
        try {
            InputStream inputStream = Files.newInputStream(mediaContent.path());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                    .contentType(MediaType.parseMediaType(mediaContent.contentType()))
                    .contentLength(mediaContent.contentLength())
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
