package com.nike.backstopper.springbootsample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts up the Backstopper Spring Boot 1.x Sample server (on port 8080 by default).
 * Сan also be used with Spring 4/Spring Boot 2.x .
 *
 * @author Andrey Tsarenko
 */
@SpringBootApplication(scanBasePackages = "com.nike")
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

}
