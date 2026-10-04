package com.scms.core.reward.service;

import java.nio.file.Path;

public record RewardImageContent(
        Path path,
        String contentType,
        long contentLength
) {
}
