package com.nike.backstopper.apierror.testing.base;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.handler.spring.config.BackstopperSpringWebMvcConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.Arrays;
import java.util.List;

/**
 * Spring config for unit/service-level testing that allows for dependency injection (e.g. injecting a validator into the test).
 *
 * @author Nic Munroe
 */
@Configuration
@ComponentScan(basePackages = "com.nike.backstopper") // Enable app-wide JSR-330 annotation-driven dependency injection.
@Import(BackstopperSpringWebMvcConfig.class)
public class TestCaseValidationSpringConfig extends WebMvcConfigurationSupport {

    public static final ApiError INVALID_COUNT_VALUE = new ApiErrorBase("INVALID_COUNT_VALUE", 99042, "Invalid count value", 400, null);
    public static final ApiError INVALID_OFFSET_VALUE = new ApiErrorBase("INVALID_OFFSET_VALUE", 99043, "Invalid offset value", 400, null);

    @Bean
    public ProjectApiErrors projectApiErrors() {
        return ProjectApiErrorsForTesting.withProjectSpecificData(
            Arrays.asList(INVALID_COUNT_VALUE, INVALID_OFFSET_VALUE),
            ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES
        );
    }

    // =========== ENABLE JSR-303 VALIDATION =================
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(new ResourceBundleMessageSource());
        return validator;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.clear();
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        converter.setObjectMapper(objectMapper);
        converters.add(converter);
        super.configureMessageConverters(converters);
    }

}
