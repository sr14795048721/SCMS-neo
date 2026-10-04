package com.scms.core.student.service;

import com.scms.core.student.domain.StudentGrade;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StudentProfileCompletionService {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^(\\d{1,3})$");
    private static final Pattern LEGACY_CLASS_NAME_PATTERN = Pattern.compile("^(\\d{1,3})鐝?$");
    private static final int MIN_CLASS_NAME = 1;
    private static final int MAX_CLASS_NAME = 30;

    private final StudentInfoRepository studentInfoRepository;

    public StudentProfileCompletionService(StudentInfoRepository studentInfoRepository) {
        this.studentInfoRepository = studentInfoRepository;
    }

    public StudentProfileCompletion getCurrentCompletion(Long userId) {
        StudentInfoEntity info = studentInfoRepository.findByUserId(userId).orElse(null);
        return evaluate(info);
    }

    public StudentProfileCompletion evaluate(StudentInfoEntity info) {
        List<String> missingRequiredFields = new ArrayList<>();

        if (info == null || isBlank(info.getDisplayName())) {
            missingRequiredFields.add("displayName");
        }
        if (info == null || isBlank(info.getStudentNo())) {
            missingRequiredFields.add("studentNo");
        }
        if (normalizeGrade(info == null ? null : info.getGrade()) == null) {
            missingRequiredFields.add("grade");
        }
        if (normalizeClassName(info == null ? null : info.getClassName()) == null) {
            missingRequiredFields.add("className");
        }

        return new StudentProfileCompletion(missingRequiredFields.isEmpty(), List.copyOf(missingRequiredFields));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String normalizeGrade(String value) {
        return StudentGrade.normalize(value);
    }

    private String normalizeClassName(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            return null;
        }

        Matcher plainMatcher = CLASS_NAME_PATTERN.matcher(normalized);
        if (plainMatcher.matches()) {
            return normalizeClassNumber(plainMatcher.group(1));
        }

        Matcher legacyMatcher = LEGACY_CLASS_NAME_PATTERN.matcher(normalized);
        if (legacyMatcher.matches()) {
            return normalizeClassNumber(legacyMatcher.group(1));
        }

        return null;
    }

    private String normalizeClassNumber(String value) {
        int numericValue = Integer.parseInt(value);
        if (numericValue < MIN_CLASS_NAME || numericValue > MAX_CLASS_NAME) {
            return null;
        }
        return String.valueOf(numericValue);
    }
}
