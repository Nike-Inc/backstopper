package com.nike.backstopper.springboot3webmvcsample;

import com.nike.backstopper.springboot3webmvcsample.config.SampleSpringboot3WebMvcSpringConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Starts up the Backstopper Spring Boot 3 Web MVC Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@Import(SampleSpringboot3WebMvcSpringConfig.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
