package com.nike.backstopper.handler.spring.webflux.componenttest;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxApiExceptionHandler;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxApiExceptionHandlerUtils;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxUnhandledExceptionHandler;
import com.nike.backstopper.handler.spring.webflux.componenttest.model.RgbColor;
import com.nike.backstopper.handler.spring.webflux.componenttest.model.SampleModel;
import com.nike.backstopper.handler.spring.webflux.config.BackstopperSpringWebFluxConfig;
import com.nike.backstopper.handler.spring.webflux.listener.SpringWebFluxApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.CONVERSION_NOT_SUPPORTED_EXCEPTION_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.ERROR_THROWING_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.FLUX_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.FLUX_RESPONSE_PAYLOAD;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.GET_SAMPLE_MODEL_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.INT_HEADER_REQUIRED_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.INT_QUERY_PARAM_REQUIRED_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.MONO_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.MONO_RESPONSE_PAYLOAD;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.NON_ERROR_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.NON_ERROR_RESPONSE_PAYLOAD;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.RESPONSE_STATUS_EX_FOR_SPECIFIC_STATUS_CODE_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.ROUTER_FUNCTION_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.ROUTER_FUNCTION_ENDPOINT_RESPONSE_PAYLOAD;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.SERVER_ERROR_EXCEPTION_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.TYPE_MISMATCH_WITH_UNEXPECTED_STATUS_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.UNHANDLED_ERROR_THROWING_ENDPOINT_PATH;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestController.UNHANDLED_EXCEPTION;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ENDPOINT_ERROR;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_RETURNED_IN_FLUX_ENDPOINT_FLUX;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_RETURNED_IN_MONO_ENDPOINT_MONO;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_RETURNED_IN_ROUTER_FUNCTION_ENDPOINT_MONO;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_RETURNED_IN_WEB_FILTER_MONO;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_THROWN_IN_FLUX_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_THROWN_IN_MONO_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_THROWN_IN_ROUTER_FUNCTION_ENDPOINT;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.ERROR_THROWN_IN_WEB_FILTER;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.INVALID_RANGE_VALUE;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.NOT_RGB_COLOR_ENUM;
import static com.nike.backstopper.handler.spring.webflux.componenttest.BackstopperSpringWebFluxComponentTest.ComponentTestProjectApiError.RGB_COLOR_CANNOT_BE_NULL;
import static com.nike.internal.util.testing.TestUtils.findFreePort;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * This component test verifies the Backstopper+Spring WebFlux integration from a black box viewpoint.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class BackstopperSpringWebFluxComponentTest {

    private static final int SERVER_PORT = findFreePort();
    private static ConfigurableApplicationContext serverAppContext;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ComponentTestProjectApiErrorsImpl projectApiErrors = new ComponentTestProjectApiErrorsImpl();
    
    private static Throwable exceptionSeenByNormalBackstopperHandler;
    private static ApiExceptionHandlerListenerResult normalBackstopperHandlingResult;
    private static Throwable exceptionSeenByUnhandledBackstopperHandler;

    @BeforeClass
    public static void beforeClass() {
        serverAppContext = SpringApplication.run(
            ComponentTestWebFluxApp.class,
            "--server.port=" + SERVER_PORT,
            // This property is necessary to allow us to override any default Spring beans that get picked up.
            //      We use this to override SpringWebfluxApiExceptionHandler and SpringWebfluxUnhandledExceptionHandler
            //      to provide some extra visibility for these tests to assert on.
            "--spring.main.allow-bean-definition-overriding=true"
        );
    }

    @AfterClass
    public static void afterClass() {
        if (serverAppContext != null) {
            SpringApplication.exit(serverAppContext);
        }
    }

    @Before
    public void beforeMethod() {
        exceptionSeenByNormalBackstopperHandler = null;
        normalBackstopperHandlingResult = null;
        exceptionSeenByUnhandledBackstopperHandler = null;
    }

    @After
    public void afterMethod() {
    }

    @Test
    public void verify_non_error_endpoint_responds_without_error() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(NON_ERROR_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.asString()).isEqualTo(NON_ERROR_RESPONSE_PAYLOAD);
    }

    @Test
    public void verify_ENDPOINT_ERROR_returned_if_error_endpoint_is_called() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(ERROR_THROWING_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, ENDPOINT_ERROR);
    }

    @Test
    public void verify_NOT_FOUND_returned_if_unknown_path_is_requested() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(UUID.randomUUID().toString())
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NOT_FOUND);
        verifyResponseStatusExceptionSeenByBackstopper(ResponseStatusException.class, 404);
    }

    @DataProvider(value = {
        "return-without-exploding   |   null",
        "throw-exception            |   ERROR_THROWN_IN_MONO_ENDPOINT",
        "return-exception-in-mono   |   ERROR_RETURNED_IN_MONO_ENDPOINT_MONO",
    }, splitBy = "\\|")
    @Test
    public void verify_mono_endpoint(
        String specialHeader, ComponentTestProjectApiError expectedError
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header(specialHeader, "true")
                .get(MONO_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        if (expectedError == null) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.asString()).isEqualTo(MONO_RESPONSE_PAYLOAD);
        }
        else {
            verifyErrorReceived(response, expectedError);
        }
    }

    @DataProvider(value = {
        "return-without-exploding   |   null",
        "throw-exception            |   ERROR_THROWN_IN_FLUX_ENDPOINT",
        "return-exception-in-flux   |   ERROR_RETURNED_IN_FLUX_ENDPOINT_FLUX",
    }, splitBy = "\\|")
    @Test
    public void verify_flux_endpoint(
        String specialHeader, ComponentTestProjectApiError expectedError
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header(specialHeader, "true")
                .get(FLUX_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        if (expectedError == null) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.asString()).isEqualTo(String.join("", FLUX_RESPONSE_PAYLOAD));
        }
        else {
            verifyErrorReceived(response, expectedError);
        }
    }

    @DataProvider(value = {
        "return-without-exploding   |   null",
        "throw-exception            |   ERROR_THROWN_IN_ROUTER_FUNCTION_ENDPOINT",
        "return-exception-in-mono   |   ERROR_RETURNED_IN_ROUTER_FUNCTION_ENDPOINT_MONO",
    }, splitBy = "\\|")
    @Test
    public void verify_router_function_endpoint(
        String specialHeader, ComponentTestProjectApiError expectedError
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header(specialHeader, "true")
                .get(ROUTER_FUNCTION_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        if (expectedError == null) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.asString()).isEqualTo(ROUTER_FUNCTION_ENDPOINT_RESPONSE_PAYLOAD);
        }
        else {
            verifyErrorReceived(response, expectedError);
        }
    }

    @DataProvider(value = {
        "throw-web-filter-exception                         |   ERROR_THROWN_IN_WEB_FILTER",
        "return-exception-in-web-filter-mono                |   ERROR_RETURNED_IN_WEB_FILTER_MONO",
        "throw-handler-filter-function-exception            |   ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION",
        "return-exception-in-handler-filter-function-mono   |   ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO",
    }, splitBy = "\\|")
    @Test
    public void verify_exploding_filter_behavior(
        String specialHeader, ComponentTestProjectApiError expectedError
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header(specialHeader, "true")
                .get(ROUTER_FUNCTION_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, expectedError);
    }

    @Test
    public void verify_METHOD_NOT_ALLOWED_returned_if_known_path_is_requested_with_invalid_http_method() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .delete(GET_SAMPLE_MODEL_ENDPOINT)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.METHOD_NOT_ALLOWED);
    }

    @Test
    public void verify_sample_get_fails_with_NO_ACCEPTABLE_REPRESENTATION_if_passed_invalid_accept_header() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .accept(ContentType.BINARY)
                .get(GET_SAMPLE_MODEL_ENDPOINT)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NO_ACCEPTABLE_REPRESENTATION);
    }

    @Test
    public void verify_sample_post_fails_with_UNSUPPORTED_MEDIA_TYPE_if_passed_invalid_content_type() throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .body(requestPayloadAsString)
                .contentType(ContentType.TEXT)
                .post(POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.UNSUPPORTED_MEDIA_TYPE);
    }
    
    @Test
    public void verify_TYPE_CONVERSION_ERROR_is_thrown_when_framework_cannot_convert_type_for_query_param() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .queryParam("requiredQueryParamValue", "not-an-integer")
                .get(INT_QUERY_PARAM_REQUIRED_ENDPOINT)
                .then()
                .log().all()
                .extract();

        ApiError expectedApiError = new ApiErrorWithMetadata(
            SampleCoreApiError.TYPE_CONVERSION_ERROR,
            // We can't expect the bad_property_name=requiredQueryParamValue metadata like we do in Spring Web MVC,
            //      because Spring WebFlux doesn't add it to the TypeMismatchException cause.
            MapBuilder.builder("bad_property_name", (Object) "requiredQueryParamValue")
                      .put("bad_property_value", "not-an-integer")
                      .put("required_location", "query_param")
                      .put("required_type", "int")
                      .build()
        );

        verifyErrorReceived(response, expectedApiError);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        TypeMismatchException tme = verifyExceptionHasCauseOfType(ex, TypeMismatchException.class);
        verifyHandlingResult(
            expectedApiError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString()),
            Pair.of("bad_property_name", tme.getPropertyName()),
            Pair.of("bad_property_value", tme.getValue().toString()),
            Pair.of("required_type", tme.getRequiredType().toString())
        );
    }

    @Test
    public void verify_TYPE_CONVERSION_ERROR_is_thrown_when_framework_cannot_convert_type_for_header() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header("requiredHeaderValue", "not-an-integer")
                .get(INT_HEADER_REQUIRED_ENDPOINT)
                .then()
                .log().all()
                .extract();

        ApiError expectedApiError = new ApiErrorWithMetadata(
            SampleCoreApiError.TYPE_CONVERSION_ERROR,
            // We can't expect the bad_property_name=requiredQueryParamValue metadata like we do in Spring Web MVC,
            //      because Spring WebFlux doesn't add it to the TypeMismatchException cause.
            MapBuilder.builder("bad_property_name", (Object) "requiredHeaderValue")
                      .put("bad_property_value", "not-an-integer")
                      .put("required_location", "header")
                      .put("required_type", "int")
                      .build()
        );

        verifyErrorReceived(response, expectedApiError);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        TypeMismatchException tme = verifyExceptionHasCauseOfType(ex, TypeMismatchException.class);
        verifyHandlingResult(
            expectedApiError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString()),
            Pair.of("bad_property_name", tme.getPropertyName()),
            Pair.of("bad_property_value", tme.getValue().toString()),
            Pair.of("required_type", tme.getRequiredType().toString())
        );
    }

    @Test
    public void verify_ResponseStatusException_with_TypeMismatchException_is_handled_generically_when_status_code_is_unexpected() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(TYPE_MISMATCH_WITH_UNEXPECTED_STATUS_ENDPOINT)
                .then()
                .log().all()
                .extract();

        // In this case the endpoint should throw a ResponseStatusException with a TypeMismatchException cause,
        //      but with an unexpected status code (403). Rather than treat it as a TypeMismatchException, we should
        //      get back a response that matches the desired status code.
        verifyErrorReceived(response, SampleCoreApiError.FORBIDDEN);
        ResponseStatusException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ResponseStatusException.class, 403
        );
        TypeMismatchException tme = verifyExceptionHasCauseOfType(ex, TypeMismatchException.class);
        verifyHandlingResult(
            SampleCoreApiError.FORBIDDEN,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage()))
        );
    }

    @Test
    public void verify_MALFORMED_REQUEST_is_thrown_when_required_query_param_is_missing_and_error_metadata_must_be_extracted_from_ex_reason() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(INT_QUERY_PARAM_REQUIRED_ENDPOINT)
                .then()
                .log().all()
                .extract();

        ApiError expectedApiError = new ApiErrorWithMetadata(
            SampleCoreApiError.MALFORMED_REQUEST,
            Pair.of("missing_param_type", "int"),
            Pair.of("missing_param_name", "requiredQueryParamValue"),
            Pair.of("required_location", "query_param")
        );

        verifyErrorReceived(response, expectedApiError);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        verifyHandlingResult(
            expectedApiError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString())
        );
        // Verify no cause, leaving the exception `reason` as the only way we could have gotten the metadata.
        assertThat(ex).hasNoCause();
        assertThat(ex.getReason()).isEqualTo("Required query parameter 'requiredQueryParamValue' is not present.");
    }

    @Test
    public void verify_MALFORMED_REQUEST_is_thrown_when_required_header_is_missing_and_error_metadata_must_be_extracted_from_ex_reason() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(INT_HEADER_REQUIRED_ENDPOINT)
                .then()
                .log().all()
                .extract();

        ApiError expectedApiError = new ApiErrorWithMetadata(
            SampleCoreApiError.MALFORMED_REQUEST,
            Pair.of("missing_param_type", "int"),
            Pair.of("missing_param_name", "requiredHeaderValue"),
            Pair.of("required_location", "header")
        );

        verifyErrorReceived(response, expectedApiError);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        verifyHandlingResult(
            expectedApiError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString())
        );
        // Verify no cause, leaving the exception `reason` as the only way we could have gotten the metadata.
        assertThat(ex).hasNoCause();
        assertThat(ex.getReason()).isEqualTo("Required header 'requiredHeaderValue' is not present.");
    }

    @Test
    public void verify_GENERIC_SERVICE_ERROR_returned_if_ServerErrorException_is_thrown() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(SERVER_ERROR_EXCEPTION_ENDPOINT_PATH, "doesNotMatter")
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.GENERIC_SERVICE_ERROR);
        ServerErrorException ex = verifyResponseStatusExceptionSeenByBackstopper(ServerErrorException.class, 500);
        verifyHandlingResult(
            SampleCoreApiError.GENERIC_SERVICE_ERROR,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString()),
            Pair.of("handler_method", ex.getHandlerMethod().toString())
        );
    }

    @Test
    public void verify_GENERIC_SERVICE_ERROR_returned_if_ResponseStatusException_with_ConversionNotSupportedException_cause_is_thrown() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(CONVERSION_NOT_SUPPORTED_EXCEPTION_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.GENERIC_SERVICE_ERROR);
        ResponseStatusException ex = verifyExceptionSeenByBackstopper(ResponseStatusException.class);
        ConversionNotSupportedException cnse = verifyExceptionHasCauseOfType(ex, ConversionNotSupportedException.class);
        verifyHandlingResult(
            SampleCoreApiError.GENERIC_SERVICE_ERROR,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("bad_property_name", cnse.getPropertyName()),
            Pair.of("bad_property_value", cnse.getValue().toString()),
            Pair.of("required_type", cnse.getRequiredType().toString())
        );
    }

    @Test
    public void verify_sample_post_fails_with_MISSING_EXPECTED_CONTENT_if_passed_empty_body() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .body("")
                .post(POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.MISSING_EXPECTED_CONTENT);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        verifyHandlingResult(
            SampleCoreApiError.MISSING_EXPECTED_CONTENT,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString())
        );
        // Verify DecodingException as the cause, leaving the exception `reason` as the only way we could have determined this case.
        assertThat(ex).hasCauseInstanceOf(DecodingException.class);
        assertThat(ex.getReason()).isEqualTo("No request body");
    }

    @Test
    public void verify_MALFORMED_REQUEST_is_returned_if_passed_bad_json_body_which_results_in_DecodingException_cause() throws IOException {
        SampleModel originalValidPayloadObj = randomizedSampleModel();
        String originalValidPayloadAsString = objectMapper.writeValueAsString(originalValidPayloadObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> badRequestPayloadAsMap = objectMapper.readValue(originalValidPayloadAsString, Map.class);
        badRequestPayloadAsMap.put("throw_manual_error", "not-a-boolean");
        String badJsonPayloadAsString = objectMapper.writeValueAsString(badRequestPayloadAsMap);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .body(badJsonPayloadAsString)
                .post(POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.MALFORMED_REQUEST);
        ServerWebInputException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ServerWebInputException.class, 400
        );
        verifyExceptionHasCauseOfType(ex, DecodingException.class);
        verifyHandlingResult(
            SampleCoreApiError.MALFORMED_REQUEST,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage())),
            Pair.of("method_parameter", ex.getMethodParameter().toString())
        );
    }

    private SampleModel randomizedSampleModel() {
        return new SampleModel(UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false);
    }

    static int nextRangeInt(int lowerBound, int upperBound) {
        return (int)Math.round(Math.random() * upperBound) + lowerBound;
    }

    static RgbColor nextRandomColor() {
        return RgbColor.values()[nextRangeInt(0, 2)];
    }

    @DataProvider(value = {
        "null   |   42  |   GREEN   |   FOO_STRING_CANNOT_BE_BLANK  |   400",
        "bar    |   -1  |   GREEN   |   INVALID_RANGE_VALUE         |   400",
        "bar    |   42  |   null    |   RGB_COLOR_CANNOT_BE_NULL    |   400",
        "bar    |   42  |   car     |   NOT_RGB_COLOR_ENUM          |   400",
        "       |   99  |   tree    |   FOO_STRING_CANNOT_BE_BLANK,INVALID_RANGE_VALUE,NOT_RGB_COLOR_ENUM   |   400",
    }, splitBy = "\\|")
    @Test
    public void verify_jsr303_validation_errors(
        String fooString,
        String rangeString,
        String rgbColorString,
        String expectedErrorsComboString,
        int expectedResponseHttpStatusCode
    ) throws JsonProcessingException {
        SampleModel requestPayload = new SampleModel(fooString, rangeString, rgbColorString, false);
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .body(requestPayloadAsString)
                .post(POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION)
                .then()
                .log().all()
                .extract();

        String[] expectedErrorsArray = expectedErrorsComboString.split(",");
        List<ApiError> expectedErrors = new ArrayList<>();
        for (String errorStr : expectedErrorsArray) {
            ApiError apiError = ComponentTestProjectApiError.valueOf(errorStr);
            String extraMetadataFieldValue = null;

            if (INVALID_RANGE_VALUE.equals(apiError)) {
                extraMetadataFieldValue = "range_0_to_42";
            }
            else if (RGB_COLOR_CANNOT_BE_NULL.equals(apiError) || NOT_RGB_COLOR_ENUM.equals(apiError)) {
                extraMetadataFieldValue = "rgb_color";
            }

            if (extraMetadataFieldValue != null) {
                apiError = new ApiErrorWithMetadata(
                    apiError,
                    MapBuilder.builder("field", (Object) extraMetadataFieldValue)
                              .build()
                );
            }

            expectedErrors.add(apiError);
        }
        verifyErrorReceived(response, expectedErrors, expectedResponseHttpStatusCode);
        verifyExceptionSeenByBackstopper(WebExchangeBindException.class);
    }

    @Test
    public void verify_GENERIC_SERVICE_ERROR_returned_if_unhandled_exception_is_thrown() {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .get(UNHANDLED_ERROR_THROWING_ENDPOINT_PATH)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.GENERIC_SERVICE_ERROR);
        // Both the normal backstopper handler and the unhandled-exception-handler should have seen the exception,
        //      but the normal one should not have handled it.
        assertThat(exceptionSeenByNormalBackstopperHandler).isSameAs(UNHANDLED_EXCEPTION);
        assertThat(exceptionSeenByUnhandledBackstopperHandler).isSameAs(UNHANDLED_EXCEPTION);
        assertThat(normalBackstopperHandlingResult.shouldHandleResponse).isFalse();
    }

    @DataProvider(value = {
        "400    |   INVALID_REQUEST",
        "401    |   UNAUTHORIZED",
        "403    |   FORBIDDEN",
        "404    |   NOT_FOUND",
        "405    |   METHOD_NOT_ALLOWED",
        "406    |   NO_ACCEPTABLE_REPRESENTATION",
        "415    |   UNSUPPORTED_MEDIA_TYPE",
        "429    |   TOO_MANY_REQUESTS",
        "500    |   GENERIC_SERVICE_ERROR",
        "503    |   TEMPORARY_SERVICE_PROBLEM",
    }, splitBy = "\\|")
    @Test
    public void verify_generic_ResponseStatusCode_exceptions_result_in_ApiError_from_project_if_status_code_is_known(
        int desiredStatusCode, SampleCoreApiError expectedError
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header("desired-status-code", desiredStatusCode)
                .get(RESPONSE_STATUS_EX_FOR_SPECIFIC_STATUS_CODE_ENDPOINT)
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, expectedError);
        ResponseStatusException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ResponseStatusException.class, desiredStatusCode
        );
        verifyHandlingResult(
            expectedError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage()))
        );
    }

    @DataProvider(value = {
        "451",
        "506"
    }, splitBy = "\\|")
    @Test
    public void verify_generic_ResponseStatusCode_exception_with_unknown_status_code_results_in_synthetic_ApiError(
        int unknownStatusCode
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .log().all()
                .when()
                .header("desired-status-code", unknownStatusCode)
                .get(RESPONSE_STATUS_EX_FOR_SPECIFIC_STATUS_CODE_ENDPOINT)
                .then()
                .log().all()
                .extract();

        String expectedErrorCodeUsed = (unknownStatusCode >= 500)
                                       ? projectApiErrors.getGenericServiceError().getErrorCode()
                                       : projectApiErrors.getGenericBadRequestApiError().getErrorCode();

        ApiError expectedError = new ApiErrorBase(
            "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_" + unknownStatusCode,
            expectedErrorCodeUsed,
            "An error occurred that resulted in response status code " + unknownStatusCode,
            unknownStatusCode
        );

        verifyErrorReceived(response, expectedError);
        ResponseStatusException ex = verifyResponseStatusExceptionSeenByBackstopper(
            ResponseStatusException.class, unknownStatusCode
        );
        verifyHandlingResult(
            expectedError,
            Pair.of("exception_message", quotesToApostrophes(ex.getMessage()))
        );
    }

    private void verifyErrorReceived(ExtractableResponse response, ApiError expectedError) {
        verifyErrorReceived(response, singleton(expectedError), expectedError.getHttpStatusCode());
    }

    private DefaultErrorDTO findErrorMatching(DefaultErrorContractDTO errorContract, ApiError desiredError) {
        for (DefaultErrorDTO error : errorContract.errors) {
            if (error.code.equals(desiredError.getErrorCode()) && error.message.equals(desiredError.getMessage()))
                return error;
        }

        return null;
    }

    private void verifyErrorReceived(ExtractableResponse response, Collection<ApiError> expectedErrors, int expectedHttpStatusCode) {
        assertThat(response.statusCode()).isEqualTo(expectedHttpStatusCode);
        try {
            DefaultErrorContractDTO errorContract = objectMapper.readValue(response.asString(), DefaultErrorContractDTO.class);
            assertThat(errorContract.error_id).isNotEmpty();
            assertThat(UUID.fromString(errorContract.error_id)).isNotNull();
            assertThat(response.header("error_uid")).isEqualTo(errorContract.error_id);
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

    private <T extends Throwable> T verifyExceptionSeenByBackstopper(Class<T> expectedClassType) {
        Throwable ex = exceptionSeenByNormalBackstopperHandler;
        assertThat(ex).isInstanceOf(expectedClassType);

        assertThat(exceptionSeenByUnhandledBackstopperHandler).isNull();

        return (T) ex;
    }

    private <T extends ResponseStatusException> T verifyResponseStatusExceptionSeenByBackstopper(
        Class<T> expectedClassType,
        int expectedStatusCode
    ) {
        T ex = verifyExceptionSeenByBackstopper(expectedClassType);

        int actualStatusCode = ex.getStatusCode().value();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);

        return ex;
    }

    private <T extends Throwable> T verifyExceptionHasCauseOfType(Throwable origEx, Class<T> expectedCauseType) {
        assertThat(origEx.getCause()).isInstanceOf(expectedCauseType);
        return (T) origEx.getCause();
    }

    private String quotesToApostrophes(String str) {
        return ApiExceptionHandlerUtils.DEFAULT_IMPL.quotesToApostrophes(str);
    }

    @SafeVarargs
    private final void verifyHandlingResult(
        ApiError expectedApiError, Pair<String, String> ... expectedExtraLoggingPairs
    ) {
        ApiExceptionHandlerListenerResult result = normalBackstopperHandlingResult;
        assertThat(result.shouldHandleResponse).isTrue();
        assertThat(result.errors).containsExactly(expectedApiError);
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrder(expectedExtraLoggingPairs);
    }

    @SpringBootApplication
    @Configuration
    @Import({BackstopperSpringWebFluxConfig.class, ComponentTestController.class })
    static class ComponentTestWebFluxApp {
        @Bean
        public ProjectApiErrors getProjectApiErrors() {
            return new ComponentTestProjectApiErrorsImpl();
        }

        @Bean
        public Validator getJsr303Validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public WebFilter explodingWebFilter() {
            return new ExplodingWebFilter();
        }

        @Bean
        public RouterFunction<ServerResponse> routerFunctionEndpoint(ComponentTestController controller) {
            return RouterFunctions
                .route(GET(ROUTER_FUNCTION_ENDPOINT_PATH), controller::routerFunctionEndpoint)
                .filter(new ExplodingHandlerFilterFunction());
        }

        @Bean
        @Primary
        public SpringWebfluxApiExceptionHandler springWebfluxApiExceptionHandler(
            ProjectApiErrors projectApiErrors,
            SpringWebFluxApiExceptionHandlerListenerList apiExceptionHandlerListeners,
            ApiExceptionHandlerUtils generalUtils,
            SpringWebfluxApiExceptionHandlerUtils springUtils,
            ObjectProvider<ViewResolver> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer
        ) {
            return new SpringWebfluxApiExceptionHandler(
                projectApiErrors, apiExceptionHandlerListeners, generalUtils, springUtils, viewResolversProvider,
                serverCodecConfigurer
            ) {
                @Override
                protected ApiExceptionHandlerListenerResult shouldHandleApiException(
                    Throwable ex
                ) {
                    exceptionSeenByNormalBackstopperHandler = ex;
                    normalBackstopperHandlingResult = super.shouldHandleApiException(ex);
                    return normalBackstopperHandlingResult;
                }
            };
        }

        @Bean
        @Primary
        public SpringWebfluxUnhandledExceptionHandler springWebfluxUnhandledExceptionHandler(
            ProjectApiErrors projectApiErrors,
            ApiExceptionHandlerUtils generalUtils,
            SpringWebfluxApiExceptionHandlerUtils springUtils,
            ObjectProvider<ViewResolver> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer
        ) {
            return new SpringWebfluxUnhandledExceptionHandler(
                projectApiErrors, generalUtils, springUtils, viewResolversProvider, serverCodecConfigurer
            ) {
                @Override
                public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
                    exceptionSeenByUnhandledBackstopperHandler = ex;
                    return super.handle(exchange, ex);
                }
            };
        }
    }

    @Controller
    static class ComponentTestController {
        static final String NON_ERROR_ENDPOINT_PATH = "/nonErrorEndpoint";
        static final String ERROR_THROWING_ENDPOINT_PATH = "/throwErrorEndpoint";
        static final String UNHANDLED_ERROR_THROWING_ENDPOINT_PATH = "/throwUnhandledErrorEndpoint";
        static final String MONO_ENDPOINT_PATH = "/monoEndpoint";
        static final String FLUX_ENDPOINT_PATH = "/fluxEndpoint";
        static final String ROUTER_FUNCTION_ENDPOINT_PATH = "/routerFunctionEndpoint";
        static final String SERVER_ERROR_EXCEPTION_ENDPOINT_PATH = "/triggerServerErrorExceptionEndpoint/{foo}";
        static final String CONVERSION_NOT_SUPPORTED_EXCEPTION_ENDPOINT_PATH =
            "/triggerConversionNotSupportedException";
        static final String INT_QUERY_PARAM_REQUIRED_ENDPOINT = "/intQueryParamRequiredEndpoint";
        static final String INT_HEADER_REQUIRED_ENDPOINT = "/intHeaderRequiredEndpoint";
        static final String TYPE_MISMATCH_WITH_UNEXPECTED_STATUS_ENDPOINT = "/typeMismatchWithUnexpectedStatusEndpoint";
        static final String GET_SAMPLE_MODEL_ENDPOINT = "/getSampleModel";
        static final String POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION =
            "/postSampleModelWithJsr303ValidationEndpoint";
        static final String RESPONSE_STATUS_EX_FOR_SPECIFIC_STATUS_CODE_ENDPOINT =
            "/responseStatusExForSpecificStatusCodeEndpoint";

        static final RuntimeException UNHANDLED_EXCEPTION = new RuntimeException("Some unhandled exception");

        static final String NON_ERROR_RESPONSE_PAYLOAD = UUID.randomUUID().toString();
        static final String MONO_RESPONSE_PAYLOAD = UUID.randomUUID().toString();
        static final List<String> FLUX_RESPONSE_PAYLOAD = Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        static final String ROUTER_FUNCTION_ENDPOINT_RESPONSE_PAYLOAD = UUID.randomUUID().toString();

        @GetMapping(NON_ERROR_ENDPOINT_PATH)
        @ResponseBody
        String nonErrorEndpoint() {
            return NON_ERROR_RESPONSE_PAYLOAD;
        }

        @GetMapping(path = GET_SAMPLE_MODEL_ENDPOINT, produces = "application/json")
        @ResponseBody
        public SampleModel getSampleModel() {
            return new SampleModel(
                UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false
            );
        }

        @PostMapping(
            path = POST_SAMPLE_MODEL_ENDPOINT_WITH_JSR_303_VALIDATION,
            consumes = "application/json",
            produces = "application/json"
        )
        @ResponseBody
        @ResponseStatus(HttpStatus.CREATED)
        public SampleModel postSampleModelWithJsr303Validation(@Valid @RequestBody SampleModel model) {
            return model;
        }

        @GetMapping(ERROR_THROWING_ENDPOINT_PATH)
        void throwErrorEndpoint() {
            throw new ApiException(ENDPOINT_ERROR);
        }

        @GetMapping(UNHANDLED_ERROR_THROWING_ENDPOINT_PATH)
        void throwUnhandledErrorEndpoint() {
            throw UNHANDLED_EXCEPTION;
        }

        @GetMapping(MONO_ENDPOINT_PATH)
        @ResponseBody
        Mono<String> monoEndpoint(ServerHttpRequest request) {
            HttpHeaders headers = request.getHeaders();

            if ("true".equals(headers.getFirst("throw-exception"))) {
                throw new ApiException(ERROR_THROWN_IN_MONO_ENDPOINT);
            }

            if ("true".equals(headers.getFirst("return-exception-in-mono"))) {
                return Mono.error(
                    new ApiException(ERROR_RETURNED_IN_MONO_ENDPOINT_MONO)
                );
            }

            return Mono.just(MONO_RESPONSE_PAYLOAD);
        }

        @GetMapping(FLUX_ENDPOINT_PATH)
        @ResponseBody
        Flux<String> fluxEndpoint(ServerHttpRequest request) {
            HttpHeaders headers = request.getHeaders();

            if ("true".equals(headers.getFirst("throw-exception"))) {
                throw new ApiException(ERROR_THROWN_IN_FLUX_ENDPOINT);
            }

            if ("true".equals(headers.getFirst("return-exception-in-flux"))) {
                return Flux.error(
                    new ApiException(ERROR_RETURNED_IN_FLUX_ENDPOINT_FLUX)
                );
            }

            return Flux.fromIterable(FLUX_RESPONSE_PAYLOAD);
        }

        Mono<ServerResponse> routerFunctionEndpoint(ServerRequest request) {
            HttpHeaders headers = request.headers().asHttpHeaders();

            if ("true".equals(headers.getFirst("throw-exception"))) {
                throw new ApiException(ERROR_THROWN_IN_ROUTER_FUNCTION_ENDPOINT);
            }

            if ("true".equals(headers.getFirst("return-exception-in-mono"))) {
                return Mono.error(
                    new ApiException(ERROR_RETURNED_IN_ROUTER_FUNCTION_ENDPOINT_MONO)
                );
            }

            return ServerResponse.ok().syncBody(ROUTER_FUNCTION_ENDPOINT_RESPONSE_PAYLOAD);
        }

        @GetMapping(path = INT_QUERY_PARAM_REQUIRED_ENDPOINT)
        @ResponseBody
        public String intQueryParamRequiredEndpoint(
            @RequestParam(name = "requiredQueryParamValue") int someRequiredQueryParam
        ) {
            return "You passed in " + someRequiredQueryParam + " for the required query param value";
        }

        @GetMapping(path = INT_HEADER_REQUIRED_ENDPOINT)
        @ResponseBody
        public String intHeaderRequiredEndpoint(
            @RequestHeader(name = "requiredHeaderValue") int someRequiredHeader
        ) {
            return "You passed in " + someRequiredHeader + " for the required header value";
        }

        // Mismatch between the path param {foo} and the name we gave the @PathVariable triggers a ServerErrorException.
        @GetMapping(path = SERVER_ERROR_EXCEPTION_ENDPOINT_PATH)
        @ResponseBody
        public String triggerServerErrorExceptionEndpoint(@PathVariable(name = "notFoo") Integer somePathParam) {
            return "we should never reach here";
        }

        // Can't figure out how to get springboot to trigger a ConversionNotSupportedException naturally, so
        //      we'll just throw one ourselves.
        @GetMapping(path = CONVERSION_NOT_SUPPORTED_EXCEPTION_ENDPOINT_PATH)
        @ResponseBody
        public String triggerConversionNotSupportedExceptionEndpoint() {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Synthetic ResponseStatusException with ConversionNotSupportedException cause and 500 status code",
                new ConversionNotSupportedException(
                    new PropertyChangeEvent(this, "somePropertyName", "oldValue", "newValue"),
                    Integer.class,
                    null
                )
            );
        }

        @GetMapping(path = TYPE_MISMATCH_WITH_UNEXPECTED_STATUS_ENDPOINT)
        @ResponseBody
        public String typeMismatchWithUnexpectedStatusEndpoint() {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Synthetic ResponseStatusException with TypeMismatchException cause and unexpected status code",
                new TypeMismatchException("doesNotMatter", int.class)
            );
        }

        @GetMapping(path = RESPONSE_STATUS_EX_FOR_SPECIFIC_STATUS_CODE_ENDPOINT)
        @ResponseBody
        public String responseStatusExForSpecificStatusCodeEndpoint(ServerHttpRequest request) {
            int desiredStatusCode = Integer.parseInt(
                request.getHeaders().getFirst("desired-status-code")
            );
            throw new ResponseStatusException(
                HttpStatus.resolve(desiredStatusCode),
                "Synthetic ResponseStatusException with specific desired status code: " + desiredStatusCode
            );
        }
    }

    enum ComponentTestProjectApiError implements ApiError {
        ENDPOINT_ERROR(99100, "An error was thrown in the endpoint", HttpStatus.BAD_REQUEST.value()),
        ERROR_THROWN_IN_MONO_ENDPOINT(
            99101, "An exception was thrown in the Mono endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_RETURNED_IN_MONO_ENDPOINT_MONO(
            99102, "An exception was returned in the Mono endpoint's Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_THROWN_IN_FLUX_ENDPOINT(
            99103, "An exception was thrown in the Flux endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_RETURNED_IN_FLUX_ENDPOINT_FLUX(
            99104, "An exception was returned in the Flus endpoint's Flux", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_THROWN_IN_ROUTER_FUNCTION_ENDPOINT(
            99105, "An exception was thrown in the RouterFunction endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_RETURNED_IN_ROUTER_FUNCTION_ENDPOINT_MONO(
            99106, "An exception was returned in the RouterFunction endpoint Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_THROWN_IN_WEB_FILTER(
            99150, "An error occurred in a WebFilter", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_RETURNED_IN_WEB_FILTER_MONO(
            99151, "An error was returned in a WebFilter Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION(
            99152, "An error occurred in a HandlerFilterFunction", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO(
            99153, "An error was returned in a HandlerFilterFunction Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ),
        FIELD_CANNOT_BE_NULL_OR_BLANK(99200, "Field cannot be null or empty", HttpStatus.BAD_REQUEST.value()),
        FOO_STRING_CANNOT_BE_BLANK(FIELD_CANNOT_BE_NULL_OR_BLANK, MapBuilder.builder("field", (Object)"foo").build()),
        INVALID_RANGE_VALUE(99110, "The range_0_to_42 field must be between 0 and 42 (inclusive)",
                            HttpStatus.BAD_REQUEST.value()),
        RGB_COLOR_CANNOT_BE_NULL(99120, "The rgb_color field must be defined", HttpStatus.BAD_REQUEST.value()),
        NOT_RGB_COLOR_ENUM(99130, "The rgb_color field value must be one of: " + Arrays.toString(RgbColor.values()),
                           HttpStatus.BAD_REQUEST.value()),;

        private final ApiError delegate;

        ComponentTestProjectApiError(ApiError delegate) {
            this.delegate = delegate;
        }


        ComponentTestProjectApiError(int errorCode, String message, int httpStatusCode) {
            this(new ApiErrorBase(
                "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), errorCode, message, httpStatusCode
            ));
        }

        ComponentTestProjectApiError(ApiError delegate, Map<String, Object> metadata) {
            this(new ApiErrorWithMetadata(delegate, metadata));
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
    static class ComponentTestProjectApiErrorsImpl extends SampleProjectApiErrorsBase {

        private static final List<ApiError> projectSpecificApiErrors =
            Arrays.asList(ComponentTestProjectApiError.values());

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

    public static class ExplodingWebFilter implements WebFilter {

        @Override
        public Mono<Void> filter(
            ServerWebExchange exchange, WebFilterChain chain
        ) {
            HttpHeaders httpHeaders = exchange.getRequest().getHeaders();

            if ("true".equals(httpHeaders.getFirst("throw-web-filter-exception"))) {
                throw ApiException
                    .newBuilder()
                    .withApiErrors(ERROR_THROWN_IN_WEB_FILTER)
                    .withExceptionMessage("Exception thrown from WebFilter")
                    .build();
            }

            if ("true".equals(httpHeaders.getFirst("return-exception-in-web-filter-mono"))) {
                return Mono.error(
                    ApiException
                        .newBuilder()
                        .withApiErrors(ERROR_RETURNED_IN_WEB_FILTER_MONO)
                        .withExceptionMessage("Exception returned from WebFilter Mono")
                        .build()
                );
            }

            return chain.filter(exchange);
        }
    }

    public static class ExplodingHandlerFilterFunction
        implements HandlerFilterFunction<ServerResponse, ServerResponse> {

        @Override
        public Mono<ServerResponse> filter(
            ServerRequest serverRequest,
            HandlerFunction<ServerResponse> handlerFunction
        ) {
            HttpHeaders httpHeaders = serverRequest.headers().asHttpHeaders();

            if ("true".equals(httpHeaders.getFirst("throw-handler-filter-function-exception"))) {
                throw ApiException
                    .newBuilder()
                    .withApiErrors(ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION)
                    .withExceptionMessage("Exception thrown from HandlerFilterFunction")
                    .build();
            }

            if ("true".equals(httpHeaders.getFirst("return-exception-in-handler-filter-function-mono"))) {
                return Mono.error(
                    ApiException
                        .newBuilder()
                        .withApiErrors(ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO)
                        .withExceptionMessage("Exception returned from HandlerFilterFunction Mono")
                        .build()
                );
            }

            return handlerFunction.handle(serverRequest);
        }
    }

}
