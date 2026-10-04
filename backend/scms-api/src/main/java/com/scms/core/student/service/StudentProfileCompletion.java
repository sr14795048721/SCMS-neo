package com.scms.core.student.service;

import java.util.List;

public record StudentProfileCompletion(
        boolean completed,
        List<String> missingRequiredFields
) {
}
