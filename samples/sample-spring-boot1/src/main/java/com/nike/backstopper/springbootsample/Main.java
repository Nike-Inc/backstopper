package com.nike.backstopper.springbootsample;

import com.nike.backstopper.springbootsample.config.SampleSpringboot1SpringConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Starts up the Backstopper Spring Boot 1 Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@Import(SampleSpringboot1SpringConfig.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
