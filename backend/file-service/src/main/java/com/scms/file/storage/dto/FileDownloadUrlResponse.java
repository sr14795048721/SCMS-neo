package com.scms.file.storage.dto;

public record FileDownloadUrlResponse(
        String objectKey,
        String url
) {
}
