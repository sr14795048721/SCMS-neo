package com.scms.core.manager.service;

import java.nio.file.Path;

public record ManagerAvatarContent(
        Path path,
        String contentType,
        long contentLength
) {
}
