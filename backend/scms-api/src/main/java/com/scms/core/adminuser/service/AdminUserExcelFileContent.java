package com.scms.core.adminuser.service;

public record AdminUserExcelFileContent(
        byte[] content,
        String fileName,
        String contentType
) {
}
