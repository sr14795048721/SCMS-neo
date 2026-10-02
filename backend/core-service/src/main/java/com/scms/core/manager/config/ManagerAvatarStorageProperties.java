package com.scms.core.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class ManagerAvatarStorageProperties {

    private String managerAvatarRoot = "./data/manager-avatars";

    public String getManagerAvatarRoot() {
        return managerAvatarRoot;
    }

    public void setManagerAvatarRoot(String managerAvatarRoot) {
        this.managerAvatarRoot = managerAvatarRoot;
    }
}
