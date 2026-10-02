package com.scms.file.common;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        "SYSTEM_ERROR",
                        "internal server error",
                        null,
                        requestId == null ? "N/A" : requestId,
                        java.time.Instant.now()
                ));
    }
}
