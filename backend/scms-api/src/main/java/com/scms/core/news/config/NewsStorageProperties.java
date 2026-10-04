package com.scms.core.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class NewsStorageProperties {

    private String newsRoot = "./data/news";

    public String getNewsRoot() {
        return newsRoot;
    }

    public void setNewsRoot(String newsRoot) {
        this.newsRoot = newsRoot;
    }
}
