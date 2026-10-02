package com.scms.core.file.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "file-service-client", url = "${file.service.url:http://file-service:8082}")
public interface FileServiceClient {

    @GetMapping("/actuator/health")
    String health();
}
