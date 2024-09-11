package com.nike.backstopper.springboot3webfluxsample;

import com.nike.backstopper.springboot3webfluxsample.config.SampleSpringboot3WebFluxSpringConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Starts up the Backstopper Spring Boot 3 WebFlux Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@Import(SampleSpringboot3WebFluxSpringConfig.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
