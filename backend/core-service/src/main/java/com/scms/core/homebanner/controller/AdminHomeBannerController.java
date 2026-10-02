package com.scms.core.homebanner.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.homebanner.domain.BannerScope;
import com.scms.core.homebanner.dto.AdminBannerRecompressResponse;
import com.scms.core.homebanner.dto.AdminHomeBannerResponse;
import com.scms.core.homebanner.dto.ReorderHomeBannersRequest;
import com.scms.core.homebanner.dto.UpdateHomeBannerRequest;
import com.scms.core.homebanner.service.HomeBannerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/home-banners")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminHomeBannerController {

    private static final BannerScope SCOPE = BannerScope.HOME;

    private final HomeBannerService homeBannerService;
    private final ApiResponseFactory responseFactory;

    public AdminHomeBannerController(HomeBannerService homeBannerService, ApiResponseFactory responseFactory) {
        this.homeBannerService = homeBannerService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminHomeBannerResponse>> list() {
        return responseFactory.success(homeBannerService.listAdmin(SCOPE));
    }

    @PostMapping
    public ApiResponse<AdminHomeBannerResponse> create() {
        return responseFactory.success(homeBannerService.create(SCOPE));
    }

    @PatchMapping("/{bannerId}")
    public ApiResponse<AdminHomeBannerResponse> update(@PathVariable("bannerId") Long bannerId,
                                                       @Valid @RequestBody UpdateHomeBannerRequest request) {
        return responseFactory.success(homeBannerService.update(SCOPE, bannerId, request));
    }

    @PostMapping(path = "/{bannerId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminHomeBannerResponse> uploadFile(@PathVariable("bannerId") Long bannerId,
                                                           @RequestParam("file") MultipartFile file) {
        return responseFactory.success(homeBannerService.uploadFile(SCOPE, bannerId, file));
    }

    @PutMapping("/order")
    public ApiResponse<List<AdminHomeBannerResponse>> reorder(@Valid @RequestBody ReorderHomeBannersRequest request) {
        return responseFactory.success(homeBannerService.reorder(SCOPE, request.ids()));
    }

    @PostMapping("/recompress")
    public ApiResponse<AdminBannerRecompressResponse> recompress() {
        return responseFactory.success(homeBannerService.recompress(SCOPE));
    }

    @DeleteMapping("/{bannerId}")
    public ApiResponse<Void> delete(@PathVariable("bannerId") Long bannerId) {
        homeBannerService.delete(SCOPE, bannerId);
        return responseFactory.success(null);
    }
}
