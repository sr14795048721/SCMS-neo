package com.scms.core.student.service;

import java.nio.file.Path;

public record StudentAvatarContent(
        Path path,
        String contentType,
        long contentLength
) {
}
