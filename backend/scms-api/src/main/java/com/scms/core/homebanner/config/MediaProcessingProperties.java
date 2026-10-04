package com.scms.core.homebanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.media")
public class MediaProcessingProperties {

    private String ffmpegPath = "ffmpeg";
    private int bannerImageMaxWidth = 1600;
    private int bannerImageMaxHeight = 900;
    private int bannerJpegQuality = 82;
    private int bannerWebpQuality = 80;
    private int bannerOptimizationVersion = 1;

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public int getBannerImageMaxWidth() {
        return bannerImageMaxWidth;
    }

    public void setBannerImageMaxWidth(int bannerImageMaxWidth) {
        this.bannerImageMaxWidth = bannerImageMaxWidth;
    }

    public int getBannerImageMaxHeight() {
        return bannerImageMaxHeight;
    }

    public void setBannerImageMaxHeight(int bannerImageMaxHeight) {
        this.bannerImageMaxHeight = bannerImageMaxHeight;
    }

    public int getBannerJpegQuality() {
        return bannerJpegQuality;
    }

    public void setBannerJpegQuality(int bannerJpegQuality) {
        this.bannerJpegQuality = bannerJpegQuality;
    }

    public int getBannerWebpQuality() {
        return bannerWebpQuality;
    }

    public void setBannerWebpQuality(int bannerWebpQuality) {
        this.bannerWebpQuality = bannerWebpQuality;
    }

    public int getBannerOptimizationVersion() {
        return bannerOptimizationVersion;
    }

    public void setBannerOptimizationVersion(int bannerOptimizationVersion) {
        this.bannerOptimizationVersion = bannerOptimizationVersion;
    }
}
