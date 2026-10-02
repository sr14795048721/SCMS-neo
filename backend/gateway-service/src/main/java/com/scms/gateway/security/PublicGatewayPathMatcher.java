package com.scms.gateway.security;

import org.springframework.http.HttpMethod;

public final class PublicGatewayPathMatcher {

    private PublicGatewayPathMatcher() {
    }

    public static boolean matches(HttpMethod method, String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        return path.equals("/error")
                || path.equals("/actuator")
                || path.startsWith("/actuator/")
                || exact(method, HttpMethod.POST, path, "/api/v1/auth/login")
                || exact(method, HttpMethod.POST, path, "/api/v1/auth/register")
                || exact(method, HttpMethod.POST, path, "/api/v1/auth/refresh")
                || exact(method, HttpMethod.POST, path, "/api/iot/v1/device-telemetry")
                || exact(method, HttpMethod.GET, path, "/api/iot/v1/device-commands/next")
                || prefix(method, HttpMethod.POST, path, "/api/iot/v1/device-commands/")
                || prefix(method, HttpMethod.GET, path, "/api/demo-observer/")
                || exact(method, HttpMethod.POST, path, "/api/v1/iot/telemetry")
                || exact(method, HttpMethod.POST, path, "/api/v1/iot/cycling-guardian/telemetry")
                || exact(method, HttpMethod.POST, path, "/api/v1/telemetry/page-view")
                || exact(method, HttpMethod.POST, path, "/api/v1/telemetry/heartbeat")
                || exact(method, HttpMethod.GET, path, "/api/v1/clubs")
                || prefix(method, HttpMethod.GET, path, "/api/v1/clubs/")
                || exact(method, HttpMethod.GET, path, "/api/v1/activities")
                || exact(method, HttpMethod.GET, path, "/api/v1/home/recommended-clubs")
                || exact(method, HttpMethod.GET, path, "/api/v1/public/home-banners")
                || prefix(method, HttpMethod.GET, path, "/api/v1/public/home-banners/")
                || exact(method, HttpMethod.GET, path, "/api/v1/public/login-banners")
                || prefix(method, HttpMethod.GET, path, "/api/v1/public/login-banners/")
                || exact(method, HttpMethod.GET, path, "/api/v1/public/news")
                || prefix(method, HttpMethod.GET, path, "/api/v1/public/news/")
                || exact(method, HttpMethod.GET, path, "/api/v1/public/app-releases/check")
                || exact(method, HttpMethod.POST, path, "/api/v1/integration/project-demo/risk-sync");
    }

    private static boolean exact(HttpMethod actualMethod, HttpMethod expectedMethod, String actualPath, String expectedPath) {
        return expectedMethod == actualMethod && expectedPath.equals(actualPath);
    }

    private static boolean prefix(
            HttpMethod actualMethod,
            HttpMethod expectedMethod,
            String actualPath,
            String expectedPathPrefix
    ) {
        return expectedMethod == actualMethod && actualPath.startsWith(expectedPathPrefix);
    }
}
