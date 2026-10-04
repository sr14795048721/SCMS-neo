package com.scms.core.adminuser.dto;

import java.util.List;

public record AdminStudentImportResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<AdminStudentImportFailureResponse> failures
) {
}
