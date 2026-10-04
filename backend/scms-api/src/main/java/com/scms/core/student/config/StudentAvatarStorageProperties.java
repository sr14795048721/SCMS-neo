package com.scms.core.student.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scms.storage")
public class StudentAvatarStorageProperties {

    private String studentAvatarRoot = "./data/student-avatars";

    public String getStudentAvatarRoot() {
        return studentAvatarRoot;
    }

    public void setStudentAvatarRoot(String studentAvatarRoot) {
        this.studentAvatarRoot = studentAvatarRoot;
    }
}

