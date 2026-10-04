package com.scms.core.reward.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.reward.dto.AdminRewardOrderResponse;
import com.scms.core.reward.service.AdminRewardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reward-orders")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminRewardOrderController {

    private final AdminRewardService adminRewardService;
    private final ApiResponseFactory responseFactory;

    public AdminRewardOrderController(AdminRewardService adminRewardService, ApiResponseFactory responseFactory) {
        this.adminRewardService = adminRewardService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminRewardOrderResponse>> list() {
        return responseFactory.success(adminRewardService.listAdminOrders());
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<AdminRewardOrderResponse> complete(@PathVariable("orderId") Long orderId) {
        return responseFactory.success(adminRewardService.completeOrder(orderId));
    }

    @PostMapping("/{orderId}/reject")
    public ApiResponse<AdminRewardOrderResponse> reject(@PathVariable("orderId") Long orderId) {
        return responseFactory.success(adminRewardService.rejectOrder(orderId));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> delete(@PathVariable("orderId") Long orderId) {
        adminRewardService.deleteOrder(orderId);
        return responseFactory.success(null);
    }
}
