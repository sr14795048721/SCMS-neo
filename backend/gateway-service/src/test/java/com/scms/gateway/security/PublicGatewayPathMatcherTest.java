package com.scms.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicGatewayPathMatcherTest {

    @Test
    void shouldTreatHomeRecommendedClubsAsPublicGetPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/home/recommended-clubs"));
    }

    @Test
    void shouldTreatClubCollectionAsPublicGetPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/clubs"));
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/clubs/42"));
    }

    @Test
    void shouldTreatActivityCollectionAsPublicGetPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/activities"));
    }

    @Test
    void shouldTreatAppReleaseCheckAsPublicGetPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/public/app-releases/check"));
    }

    @Test
    void shouldTreatIotTelemetryIngestAsPublicPostPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.POST, "/api/iot/v1/device-telemetry"));
    }

    @Test
    void shouldTreatEsp32TelemetryIngestAsPublicPostPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.POST, "/api/v1/iot/telemetry"));
    }

    @Test
    void shouldTreatCyclingGuardianTelemetryIngestAsPublicPostPath() {
        assertTrue(PublicGatewayPathMatcher.matches(HttpMethod.POST, "/api/v1/iot/cycling-guardian/telemetry"));
    }

    @Test
    void shouldRejectProtectedAdminPathWithoutWhitelistEntry() {
        assertFalse(PublicGatewayPathMatcher.matches(HttpMethod.GET, "/api/v1/admin/clubs"));
    }
}
