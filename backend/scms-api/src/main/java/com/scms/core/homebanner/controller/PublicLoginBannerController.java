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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/login-banners")
public class PublicLoginBannerController {

    private static final BannerScope SCOPE = BannerScope.LOGIN;

    private final HomeBannerService homeBannerService;
    private final ApiResponseFactory responseFactory;

    public PublicLoginBannerController(HomeBannerService homeBannerService, ApiResponseFactory responseFactory) {
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
            return PublicHomeBannerController.buildVideoResponse(mediaContent, rangeHeader);
        }
        return PublicHomeBannerController.buildBinaryResponse(mediaContent);
    }

    @GetMapping("/{bannerId}/poster")
    public ResponseEntity<InputStreamResource> poster(@PathVariable("bannerId") Long bannerId) {
        return PublicHomeBannerController.buildBinaryResponse(homeBannerService.resolvePosterContent(SCOPE, bannerId));
    }
}
