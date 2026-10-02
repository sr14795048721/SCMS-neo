package com.scms.core.common.web;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMissing() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Required request body is missing: public test.Controller.method()"
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableBody(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertEquals("request body is required", response.getBody().message());
    }

    @Test
    void shouldReturnValidationErrorWhenRequestBodyIsMalformed() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character"
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableBody(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertEquals("request body is invalid", response.getBody().message());
    }
}
