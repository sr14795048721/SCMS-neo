package com.scms.core.homebanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class BannerStorageProperties {

    private String bannerRoot = "./data/banners";

    public String getBannerRoot() {
        return bannerRoot;
    }

    public void setBannerRoot(String bannerRoot) {
        this.bannerRoot = bannerRoot;
    }
}
