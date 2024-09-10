package com.nike.backstopper.springsample.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.spring.config.BackstopperSpringWebMvcConfig;
import com.nike.backstopper.springsample.controller.SampleController;
import com.nike.backstopper.springsample.error.SampleProjectApiErrorsImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Simple Spring Web MVC config for the sample app. The {@link ProjectApiErrors} and {@link Validator} beans defined
 * in this class are needed for autowiring Backstopper and the {@link ProjectApiErrors} in particular allows you
 * to specify project-specific errors and behaviors.
 *
 * <p>NOTE: This integrates Backstopper by {@link Import}ing {@link BackstopperSpringWebMvcConfig}. Alternatively,
 * you could integrate Backstopper by component scanning all of the {@code com.nike.backstopper} package and its
 * subpackages, e.g. by annotating with
 * {@link org.springframework.context.annotation.ComponentScan @ComponentScan(basePackages = "com.nike.backstopper")}.
 *
 * @author Nic Munroe
 */
@Configuration
@Import({
    // Import core Backstopper+Spring support.
    BackstopperSpringWebMvcConfig.class,
    // Import this sample app's controller.
    SampleController.class
})
@EnableWebMvc
@SuppressWarnings("unused")
public class SampleWebMvcConfig implements WebMvcConfigurer {

    /**
     * @return The {@link ProjectApiErrors} to use for this sample app.
     */
    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    /**
     * NOTE: Spring uses its own system for JSR 303 validation, so this {@code @Bean} is only here to satisfy the
     * dependency injection requirements of {@link com.nike.backstopper.service.ClientDataValidationService} and
     * {@link com.nike.backstopper.service.FailFastServersideValidationService}. With this {@link Validator} defined
     * you could inject one of those services into your controllers and use it as advertised. If you were never going
     * to use those services you could have this return {@link
     * com.nike.backstopper.service.NoOpJsr303Validator#SINGLETON_IMPL} instead and not have to pull in any JSR 303
     * implementation dependency into your project.
     */
    @Bean
    @SuppressWarnings("resource")
    public Validator getJsr303Validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
