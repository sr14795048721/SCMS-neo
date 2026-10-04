package com.scms.core.adminuser.dto;

public record AdminStudentImportFailureResponse(
        int rowNumber,
        String email,
        String reasonCode,
        String reasonMessage
) {
}
