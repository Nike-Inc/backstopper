package com.nike.backstopper.springboot2webmvcsample.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.springboot.config.BackstopperSpringboot2WebMvcConfig;
import com.nike.backstopper.handler.springboot.controller.BackstopperSpringboot2ContainerErrorController;
import com.nike.backstopper.springboot2webmvcsample.error.SampleProjectApiError;
import com.nike.backstopper.springboot2webmvcsample.error.SampleProjectApiErrorsImpl;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Simple Spring Boot config for the sample app. The {@link ProjectApiErrors} and {@link Validator} beans defined
 * in this class are needed for autowiring Backstopper and the {@link ProjectApiErrors} in particular allows you
 * to specify project-specific errors and behaviors.
 *
 * <p>NOTE: This integrates Backstopper by {@link Import}ing {@link BackstopperSpringboot2WebMvcConfig}. Alternatively,
 * you could integrate Backstopper by component scanning all of the {@code com.nike.backstopper} package and its
 * subpackages, e.g. by annotating with
 * {@link org.springframework.context.annotation.ComponentScan @ComponentScan(basePackages = "com.nike.backstopper")}.
 *
 * @author Nic Munroe
 */
@Configuration
@Import(BackstopperSpringboot2WebMvcConfig.class)
// Instead of @Import(BackstopperSpringboot2WebMvcConfig.class), you could component scan the com.nike.backstopper
//      package like this if you prefer component scanning: @ComponentScan(basePackages = "com.nike.backstopper")
public class SampleSpringboot2WebMvcSpringConfig {

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
    public Validator getJsr303Validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Registers a custom {@link ExplodingFilter} Servlet filter at the highest precedence that will throw an exception
     * when the request contains a special header. This exception will be thrown outside of Springboot, and can
     * be used to exercise the {@link BackstopperSpringboot2ContainerErrorController}. You wouldn't want this in
     * a real app.
     */
    @Bean
    public FilterRegistrationBean explodingServletFilter() {
        FilterRegistrationBean<ExplodingFilter> frb = new FilterRegistrationBean<>(new ExplodingFilter());
        frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return frb;
    }

    private static class ExplodingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
        ) throws ServletException, IOException {
            if ("true".equals(request.getHeader("throw-servlet-filter-exception"))) {
                throw ApiException
                    .newBuilder()
                    .withApiErrors(SampleProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING)
                    .withExceptionMessage("Exception thrown from Servlet Filter outside Spring")
                    .build();
            }
            filterChain.doFilter(request, response);
        }

    }
}
