package com.scms.core.yinzhi.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.yinzhi.dto.YinzhiDeviceCommandAckRequest;
import com.scms.core.yinzhi.dto.YinzhiDeviceCommandPollResponse;
import com.scms.core.yinzhi.service.YinzhiAiInspectionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iot/v1/device-commands")
public class YinzhiDeviceCommandController {

    private final YinzhiAiInspectionQueryService queryService;
    private final ApiResponseFactory responseFactory;

    public YinzhiDeviceCommandController(
            YinzhiAiInspectionQueryService queryService,
            ApiResponseFactory responseFactory
    ) {
        this.queryService = queryService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/next")
    public ApiResponse<YinzhiDeviceCommandPollResponse> next(
            @RequestParam("deviceId") String deviceId
    ) {
        return responseFactory.success(queryService.pollNextCommand(deviceId));
    }

    @PostMapping("/{commandId}/ack")
    public ApiResponse<YinzhiDeviceCommandPollResponse> ack(
            @PathVariable("commandId") Long commandId,
            @RequestBody(required = false) YinzhiDeviceCommandAckRequest request
    ) {
        return responseFactory.success(queryService.ackCommand(commandId, request));
    }
}
