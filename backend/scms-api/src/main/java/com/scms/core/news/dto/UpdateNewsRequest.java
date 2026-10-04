package com.scms.core.news.dto;

public record UpdateNewsRequest(
        String title,
        String markdownContent,
        String status
) {
}
