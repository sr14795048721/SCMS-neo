package com.scms.core.student.domain;

public enum StudentGrade {
    HIGH_1("高一"),
    HIGH_2("高二"),
    HIGH_3("高三");

    private final String legacyLabel;

    StudentGrade(String legacyLabel) {
        this.legacyLabel = legacyLabel;
    }

    public String code() {
        return name();
    }

    public static String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        for (StudentGrade grade : values()) {
            if (grade.code().equalsIgnoreCase(normalized) || grade.legacyLabel.equals(normalized)) {
                return grade.code();
            }
        }

        return null;
    }
}
