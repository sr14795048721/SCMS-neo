package com.scms.core.clubappworkspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.app-workspace")
public class AppWorkspaceProperties {

    private final Storage storage = new Storage();

    public Storage getStorage() {
        return storage;
    }

    public static class Storage {

        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public boolean isConfigured() {
            return !endpoint.isBlank()
                    && !accessKey.isBlank()
                    && !secretKey.isBlank()
                    && !bucket.isBlank();
        }
    }
}
