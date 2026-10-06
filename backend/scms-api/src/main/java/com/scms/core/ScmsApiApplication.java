package com.scms.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScmsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmsApiApplication.class, args);
    }
}
