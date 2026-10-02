package com.scms.core.homebanner.domain;

public enum BannerScope {
    HOME("home-banners"),
    LOGIN("login-banners");

    private final String pathSegment;

    BannerScope(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String publicBasePath() {
        return "/api/v1/public/" + pathSegment;
    }
}
