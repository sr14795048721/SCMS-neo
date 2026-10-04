package com.scms.core.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.ai")
public class AiProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.siliconflow.cn";
    private String apiKey = "";
    private String model = "Pro/deepseek-ai/DeepSeek-V3.1-Terminus";
    private String systemPrompt = "You are a helpful assistant";
    private int timeoutSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isConfigured() {
        return enabled
                && !baseUrl.isBlank()
                && !apiKey.isBlank()
                && !model.isBlank()
                && !systemPrompt.isBlank()
                && timeoutSeconds > 0;
    }
}
