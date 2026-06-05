package com.example.studentfees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.studentfees.config.CcavenueProperties;

@SpringBootApplication
@EnableConfigurationProperties(CcavenueProperties.class)
public class StudentFeesApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentFeesApplication.class, args);
    }
}
