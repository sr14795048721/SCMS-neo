package com.scms.core.reward.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class RewardImageStorageProperties {

    private String rewardImageRoot = "./data/core-reward-images";

    public String getRewardImageRoot() {
        return rewardImageRoot;
    }

    public void setRewardImageRoot(String rewardImageRoot) {
        this.rewardImageRoot = rewardImageRoot;
    }
}
