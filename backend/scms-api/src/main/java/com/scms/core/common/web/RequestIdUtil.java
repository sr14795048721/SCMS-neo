package com.scms.core.common.web;

import org.slf4j.MDC;

import java.util.UUID;

public final class RequestIdUtil {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTR = "requestId";

    private RequestIdUtil() {
    }

    public static String resolveFromMdcOrGenerate() {
        String requestId = MDC.get(REQUEST_ID_ATTR);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            MDC.put(REQUEST_ID_ATTR, requestId);
        }
        return requestId;
    }
}
