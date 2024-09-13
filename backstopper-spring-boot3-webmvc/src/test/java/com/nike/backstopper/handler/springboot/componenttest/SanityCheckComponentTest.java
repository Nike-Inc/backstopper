package com.nike.backstopper.handler.springboot.componenttest;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.springboot.config.BackstopperSpringboot3WebMvcConfig;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.response.ExtractableResponse;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static com.nike.backstopper.handler.springboot.componenttest.SanityCheckComponentTest.SanityCheckController.ERROR_THROWING_ENDPOINT_PATH;
import static com.nike.backstopper.handler.springboot.componenttest.SanityCheckComponentTest.SanityCheckController.NON_ERROR_ENDPOINT_PATH;
import static com.nike.backstopper.handler.springboot.componenttest.SanityCheckComponentTest.SanityCheckController.NON_ERROR_RESPONSE_PAYLOAD;
import static com.nike.internal.util.testing.TestUtils.findFreePort;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This component test is just a sanity check on the few features this module provides above and beyond the regular
 * backstopper+spring stuff. The samples and testonly modules have more comprehensive component tests.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class SanityCheckComponentTest {

    private static final int SERVER_PORT = findFreePort();
    private static ConfigurableApplicationContext serverAppContext;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeClass
    public static void beforeClass() {
        serverAppContext = SpringApplication.run(SanitcyCheckComponentTestApp.class, "--server.port=" + SERVER_PORT);
    }

    @AfterClass
    public static void afterClass() {
        SpringApplication.exit(serverAppContext);
    }

    @Before
    public void beforeMethod() {
    }

    @After
    public void afterMethod() {
    }

    private void verifyErrorReceived(ExtractableResponse<?> response, ApiError expectedError) {
        verifyErrorReceived(response, singleton(expectedError), expectedError.getHttpStatusCode());
    }

    private DefaultErrorDTO findErrorMatching(DefaultErrorContractDTO errorContract, ApiError desiredError) {
        for (DefaultErrorDTO error : errorContract.errors) {
            if (error.code.equals(desiredError.getErrorCode()) && error.message.equals(desiredError.getMessage()))
                return error;
        }

        return null;
    }

    private void verifyErrorReceived(ExtractableResponse<?> response, Collection<ApiError> expectedErrors, int expectedHttpStatusCode) {
        assertThat(response.statusCode()).isEqualTo(expectedHttpStatusCode);
        try {
            DefaultErrorContractDTO errorContract = objectMapper.readValue(response.asString(), DefaultErrorContractDTO.class);
            assertThat(errorContract.error_id).isNotEmpty();
            assertThat(UUID.fromString(errorContract.error_id)).isNotNull();
            assertThat(errorContract.errors).hasSameSizeAs(expectedErrors);
            for (ApiError apiError : expectedErrors) {
                DefaultErrorDTO matchingError = findErrorMatching(errorContract, apiError);
                assertThat(matchingError).isNotNull();
                assertThat(matchingError.code).isEqualTo(apiError.getErrorCode());
                assertThat(matchingError.message).isEqualTo(apiError.getMessage());
                assertThat(matchingError.metadata).isEqualTo(apiError.getMetadata());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void verify_non_error_endpoint_responds_without_error() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(NON_ERROR_ENDPOINT_PATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo(NON_ERROR_RESPONSE_PAYLOAD);
    }

    @Test
    public void verify_ENDPOINT_ERROR_returned_if_error_endpoint_is_called() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(ERROR_THROWING_ENDPOINT_PATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SanityCheckProjectApiError.ENDPOINT_ERROR);
    }

    @Test
    public void verify_NOT_FOUND_returned_if_unknown_path_is_requested() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(UUID.randomUUID().toString())
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NOT_FOUND);
    }

    @Test
    public void verify_ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING_returned_if_servlet_filter_trigger_occurs() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(NON_ERROR_ENDPOINT_PATH)
                .header("throw-servlet-filter-exception", "true")
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SanityCheckProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING);
    }

    @SpringBootApplication
    @Configuration
    @Import({BackstopperSpringboot3WebMvcConfig.class, SanityCheckController.class })
    @SuppressWarnings("unused")
    static class SanitcyCheckComponentTestApp {
        @Bean
        public ProjectApiErrors getProjectApiErrors() {
            return new SanityCheckProjectApiErrorsImpl();
        }

        @Bean
        public Validator getJsr303Validator() {
            //noinspection resource
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        public FilterRegistrationBean<?> explodingServletFilter() {
            FilterRegistrationBean<?> frb = new FilterRegistrationBean<>(new ExplodingFilter());
            frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return frb;
        }

        public static class ExplodingFilter extends OncePerRequestFilter {

            @Override
            protected void doFilterInternal(
                HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain
            ) throws ServletException, IOException {
                if ("true".equals(request.getHeader("throw-servlet-filter-exception"))) {
                    throw ApiException
                        .newBuilder()
                        .withApiErrors(SanityCheckProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING)
                        .withExceptionMessage("Exception thrown from Servlet Filter outside Spring")
                        .build();
                }
                filterChain.doFilter(request, response);
            }

        }
    }

    @Controller
    @SuppressWarnings("unused")
    static class SanityCheckController {
        public static final String NON_ERROR_ENDPOINT_PATH = "/nonErrorEndpoint";
        public static final String ERROR_THROWING_ENDPOINT_PATH = "/throwErrorEndpoint";

        public static final String NON_ERROR_RESPONSE_PAYLOAD = UUID.randomUUID().toString();

        @GetMapping(NON_ERROR_ENDPOINT_PATH)
        @ResponseBody
        public String nonErrorEndpoint() {
            return NON_ERROR_RESPONSE_PAYLOAD;
        }

        @GetMapping(ERROR_THROWING_ENDPOINT_PATH)
        public void throwErrorEndpoint() {
            throw new ApiException(SanityCheckProjectApiError.ENDPOINT_ERROR);
        }
    }

    enum SanityCheckProjectApiError implements ApiError {
        ENDPOINT_ERROR(99100, "An error was thrown in the endpoint", HttpStatus.BAD_REQUEST.value()),
        ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING(
            99150, "An error occurred in a Servlet Filter outside Spring", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        private final ApiError delegate;

        SanityCheckProjectApiError(ApiError delegate) {
            this.delegate = delegate;
        }


        SanityCheckProjectApiError(int errorCode, String message, int httpStatusCode) {
            this(new ApiErrorBase(
                "delegated-to-enum-wrapper-" + UUID.randomUUID(), errorCode, message, httpStatusCode
            ));
        }

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public String getErrorCode() {
            return delegate.getErrorCode();
        }

        @Override
        public String getMessage() {
            return delegate.getMessage();
        }

        @Override
        public int getHttpStatusCode() {
            return delegate.getHttpStatusCode();
        }

        @Override
        public Map<String, Object> getMetadata() {
            return delegate.getMetadata();
        }

    }

    @Singleton
    static class SanityCheckProjectApiErrorsImpl extends SampleProjectApiErrorsBase {

        private static final List<ApiError> projectSpecificApiErrors =
            Arrays.asList(SanityCheckProjectApiError.values());

        // Set the valid range of non-core error codes for this project to be 99100-99200.
        private static final ProjectSpecificErrorCodeRange errorCodeRange =
            ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES;

        @Override
        protected List<ApiError> getProjectSpecificApiErrors() {
            return projectSpecificApiErrors;
        }

        @Override
        protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
            return errorCodeRange;
        }

    }
}
