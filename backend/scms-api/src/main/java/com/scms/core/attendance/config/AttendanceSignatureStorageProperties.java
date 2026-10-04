package com.scms.core.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class AttendanceSignatureStorageProperties {

    private String attendanceSignatureRoot = "./data/attendance-signatures";

    public String getAttendanceSignatureRoot() {
        return attendanceSignatureRoot;
    }

    public void setAttendanceSignatureRoot(String attendanceSignatureRoot) {
        this.attendanceSignatureRoot = attendanceSignatureRoot;
    }
}
