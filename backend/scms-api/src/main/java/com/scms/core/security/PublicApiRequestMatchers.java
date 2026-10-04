package com.scms.core.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

public final class PublicApiRequestMatchers {

    private static final RequestMatcher[] MATCHERS = new RequestMatcher[]{
            exact(null, "/error"),
            exact(null, "/actuator"),
            prefix(null, "/actuator/"),
            exact("POST", "/api/v1/auth/login"),
            exact("POST", "/api/v1/auth/refresh"),
            exact("POST", "/api/v1/auth/register"),
            exact("POST", "/api/iot/v1/device-telemetry"),
            exact("GET", "/api/iot/v1/device-commands/next"),
            prefix("POST", "/api/iot/v1/device-commands/"),
            prefix("GET", "/api/demo-observer/"),
            exact("POST", "/api/v1/iot/telemetry"),
            exact("POST", "/api/v1/iot/cycling-guardian/telemetry"),
            exact("POST", "/api/v1/telemetry/page-view"),
            exact("POST", "/api/v1/telemetry/heartbeat"),
            exact("GET", "/api/v1/clubs"),
            exact("GET", "/api/v1/activities"),
            exact("GET", "/api/v1/home/recommended-clubs"),
            exact("GET", "/api/v1/public/home-banners"),
            prefix("GET", "/api/v1/public/home-banners/"),
            exact("GET", "/api/v1/public/login-banners"),
            prefix("GET", "/api/v1/public/login-banners/"),
            exact("GET", "/api/v1/public/news"),
            prefix("GET", "/api/v1/public/news/"),
            prefix("GET", "/api/v1/public/attendance/sessions/"),
            prefix("POST", "/api/v1/public/attendance/sessions/"),
            exact("GET", "/api/v1/public/app-releases/check"),
            exact("POST", "/api/v1/integration/project-demo/risk-sync")
    };

    private PublicApiRequestMatchers() {
    }

    public static RequestMatcher[] all() {
        return MATCHERS.clone();
    }

    public static boolean matches(HttpServletRequest request) {
        return Arrays.stream(MATCHERS).anyMatch((matcher) -> matcher.matches(request));
    }

    private static RequestMatcher exact(String method, String path) {
        return request -> methodMatches(request, method) && path.equals(request.getRequestURI());
    }

    private static RequestMatcher prefix(String method, String pathPrefix) {
        return request -> methodMatches(request, method) && request.getRequestURI().startsWith(pathPrefix);
    }

    private static boolean methodMatches(HttpServletRequest request, String method) {
        return method == null || method.equalsIgnoreCase(request.getMethod());
    }
}
