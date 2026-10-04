package com.scms.core.notification.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.notification.domain.NotificationStatus;
import com.scms.core.notification.dto.NotificationFeedResponse;
import com.scms.core.notification.dto.NotificationResponse;
import com.scms.core.notification.dto.NotificationSummaryResponse;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;
    private final ApiResponseFactory responseFactory;

    public NotificationController(NotificationService notificationService,
                                  CurrentUserProvider currentUserProvider,
                                  ApiResponseFactory responseFactory) {
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/me")
    public ApiResponse<List<NotificationResponse>> listMyNotifications(
            @RequestParam(name = "status", defaultValue = "") String status
    ) {
        Long userId = currentUserProvider.getRequiredUser().userId();
        return responseFactory.success(notificationService.listLatest(userId, parseStatus(status)));
    }

    @GetMapping("/me/summary")
    public ApiResponse<NotificationSummaryResponse> getMyNotificationSummary() {
        Long userId = currentUserProvider.getRequiredUser().userId();
        return responseFactory.success(notificationService.getSummary(userId));
    }

    @GetMapping("/me/feed")
    public ApiResponse<NotificationFeedResponse> getMyNotificationFeed(
            @RequestParam(name = "status") String status,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size
    ) {
        Long userId = currentUserProvider.getRequiredUser().userId();
        return responseFactory.success(notificationService.getFeed(userId, parseRequiredStatus(status), page, size));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable("notificationId") Long notificationId) {
        Long userId = currentUserProvider.getRequiredUser().userId();
        return responseFactory.success(notificationService.markRead(userId, notificationId));
    }

    private NotificationStatus parseStatus(String status) {
        String normalized = String.valueOf(status == null ? "" : status).trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return NotificationStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid notification status");
        }
    }

    private NotificationStatus parseRequiredStatus(String status) {
        NotificationStatus parsed = parseStatus(status);
        if (parsed == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "notification status is required");
        }
        return parsed;
    }
}
