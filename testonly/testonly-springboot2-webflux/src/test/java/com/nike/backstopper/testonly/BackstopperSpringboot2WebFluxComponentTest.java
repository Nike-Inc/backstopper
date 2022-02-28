package com.nike.backstopper.testonly;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import serverconfig.classpathscan.Springboot2WebFluxClasspathScanConfig;
import serverconfig.directimport.Springboot2WebFluxDirectImportConfig;
import testonly.componenttest.spring.reusable.error.SampleProjectApiError;
import testonly.componenttest.spring.reusable.model.RgbColor;
import testonly.componenttest.spring.reusable.model.SampleModel;

import static com.nike.internal.util.testing.TestUtils.findFreePort;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static testonly.componenttest.spring.reusable.controller.SampleController.CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH;
import static testonly.componenttest.spring.reusable.controller.SampleController.SAMPLE_PATH;
import static testonly.componenttest.spring.reusable.controller.SampleController.TRIGGER_UNHANDLED_ERROR_SUBPATH;
import static testonly.componenttest.spring.reusable.controller.SampleController.WITH_REQUIRED_QUERY_PARAM_SUBPATH;
import static testonly.componenttest.spring.reusable.error.SampleProjectApiError.FOO_STRING_CANNOT_BE_BLANK;
import static testonly.componenttest.spring.reusable.error.SampleProjectApiError.INVALID_RANGE_VALUE;
import static testonly.componenttest.spring.reusable.error.SampleProjectApiError.NOT_RGB_COLOR_ENUM;
import static testonly.componenttest.spring.reusable.error.SampleProjectApiError.RGB_COLOR_CANNOT_BE_NULL;
import static testonly.componenttest.spring.reusable.testutil.TestUtils.randomizedSampleModel;
import static testonly.componenttest.spring.reusable.testutil.TestUtils.verifyErrorReceived;
import static testonly.componenttest.spring.webflux.controller.SampleWebFluxController.FLUX_ERROR_SUBPATH;
import static testonly.componenttest.spring.webflux.controller.SampleWebFluxController.MONO_ERROR_SUBPATH;
import static testonly.componenttest.spring.webflux.controller.SampleWebFluxController.SAMPLE_FLUX_SUBPATH;
import static testonly.componenttest.spring.webflux.controller.SampleWebFluxController.SAMPLE_FROM_ROUTER_FUNCTION_PATH;

/**
 * Component test to verify that the functionality of {@code backstopper-spring-web-flux} works as expected in a
 * Spring Boot 2 WebFlux environment, for both classpath-scanning and direct-import Backstopper configuration use cases.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class BackstopperSpringboot2WebFluxComponentTest {

    private static final int CLASSPATH_SCAN_SERVER_PORT = findFreePort();
    private static final int DIRECT_IMPORT_SERVER_PORT = findFreePort();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static ConfigurableApplicationContext classpathScanServerAppContext;
    private static ConfigurableApplicationContext directImportServerAppContext;

    @BeforeClass
    public static void beforeClass() {
        assertThat(CLASSPATH_SCAN_SERVER_PORT).isNotEqualTo(DIRECT_IMPORT_SERVER_PORT);
        classpathScanServerAppContext = SpringApplication.run(
            Springboot2WebFluxClasspathScanConfig.class, "--server.port=" + CLASSPATH_SCAN_SERVER_PORT
        );
        directImportServerAppContext = SpringApplication.run(
            Springboot2WebFluxDirectImportConfig.class, "--server.port=" + DIRECT_IMPORT_SERVER_PORT
        );
    }

    @AfterClass
    public static void afterClass() {
        SpringApplication.exit(classpathScanServerAppContext);
        SpringApplication.exit(directImportServerAppContext);
    }

    @SuppressWarnings("unused")
    private enum ServerScenario {
        CLASSPATH_SCAN_SERVER(CLASSPATH_SCAN_SERVER_PORT),
        DIRECT_IMPORT_SERVER(DIRECT_IMPORT_SERVER_PORT);

        public final int serverPort;

        ServerScenario(int serverPort) {
            this.serverPort = serverPort;
        }
    }

    @DataProvider
    public static List<List<ServerScenario>> serverScenarioDataProvider() {
        return Stream.of(ServerScenario.values()).map(Collections::singletonList).collect(Collectors.toList());
    }

    // *************** SUCCESSFUL (NON ERROR) CALLS ******************
    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_basic_sample_get(ServerScenario scenario) throws IOException {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        SampleModel responseBody = objectMapper.readValue(response.asString(), SampleModel.class);
        verifyNewSampleModel(responseBody);
    }

    private void verifyNewSampleModel(SampleModel sampleModel) {
        assertThat(sampleModel).isNotNull();
        assertThat(sampleModel.foo).isNotEmpty();
        assertThat(sampleModel.range_0_to_42).isNotEmpty();
        assertThat(Integer.parseInt(sampleModel.range_0_to_42)).isBetween(0, 42);
        assertThat(sampleModel.rgb_color).isNotEmpty();
        assertThat(RgbColor.toRgbColor(sampleModel.rgb_color)).isNotNull();
        assertThat(sampleModel.throw_manual_error).isFalse();
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_basic_sample_post(ServerScenario scenario) throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .contentType(ContentType.JSON)
                .basePath(SAMPLE_PATH)
                .body(requestPayloadAsString)
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(201);
        SampleModel responseBody = objectMapper.readValue(response.asString(), SampleModel.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.foo).isEqualTo(requestPayload.foo);
        assertThat(responseBody.range_0_to_42).isEqualTo(requestPayload.range_0_to_42);
        assertThat(responseBody.rgb_color).isEqualTo(requestPayload.rgb_color);
        assertThat(responseBody.throw_manual_error).isEqualTo(requestPayload.throw_manual_error);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_router_function_sample_get(ServerScenario scenario) throws IOException {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_FROM_ROUTER_FUNCTION_PATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        SampleModel responseBody = objectMapper.readValue(response.asString(), SampleModel.class);
        verifyNewSampleModel(responseBody);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_flux_sample_get(ServerScenario scenario) throws IOException {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + SAMPLE_FLUX_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        List<SampleModel> responseBody = objectMapper.readValue(
            response.asString(), new TypeReference<List<SampleModel>>(){}
        );

        assertThat(responseBody).hasSizeGreaterThan(1);
        responseBody.forEach(this::verifyNewSampleModel);
    }

    // *************** JSR 303 AND ENDPOINT ERRORS ******************

    @SuppressWarnings("unused")
    private enum Jsr303SampleModelValidationScenario {
        BLANK_FIELD_VIOLATION(
            new SampleModel("", "42", "GREEN", false),
            singletonList(FOO_STRING_CANNOT_BE_BLANK)
        ),
        INVALID_RANGE_VIOLATION(
            new SampleModel("bar", "-1", "GREEN", false),
            singletonList(INVALID_RANGE_VALUE)
        ),
        NULL_FIELD_VIOLATION(
            new SampleModel("bar", "42", null, false),
            singletonList(RGB_COLOR_CANNOT_BE_NULL)
        ),
        STRING_CONVERTS_TO_CLASSTYPE_VIOLATION(
            new SampleModel("bar", "42", "car", false),
            singletonList(NOT_RGB_COLOR_ENUM)
        ),
        MULTIPLE_VIOLATIONS(
            new SampleModel("  \n\r\t  ", "99", "tree", false),
            Arrays.asList(FOO_STRING_CANNOT_BE_BLANK, INVALID_RANGE_VALUE, NOT_RGB_COLOR_ENUM)
        );

        public final SampleModel model;
        public final List<ApiError> expectedErrors;

        Jsr303SampleModelValidationScenario(
            SampleModel model, List<ApiError> expectedErrors
        ) {
            this.model = model;
            this.expectedErrors = expectedErrors;
        }
    }

    @DataProvider
    public static List<List<Object>> jsr303ValidationErrorScenariosDataProvider() {
        List<List<Object>> result = new ArrayList<>();
        for (Jsr303SampleModelValidationScenario violationScenario : Jsr303SampleModelValidationScenario.values()) {
            for (ServerScenario serverScenario : ServerScenario.values()) {
                result.add(Arrays.asList(violationScenario, serverScenario));
            }
        }
        return result;
    }

    @UseDataProvider("jsr303ValidationErrorScenariosDataProvider")
    @Test
    public void verify_jsr303_validation_errors(
        Jsr303SampleModelValidationScenario violationScenario, ServerScenario serverScenario
    ) throws JsonProcessingException {
        String requestPayloadAsString = objectMapper.writeValueAsString(violationScenario.model);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(serverScenario.serverPort)
                .contentType(ContentType.JSON)
                .basePath(SAMPLE_PATH)
                .body(requestPayloadAsString)
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        List<ApiError> expectedErrors = new ArrayList<>();
        for (ApiError expectedApiError : violationScenario.expectedErrors) {
            String extraMetadataFieldValue = null;

            if (INVALID_RANGE_VALUE.equals(expectedApiError)) {
                extraMetadataFieldValue = "range_0_to_42";
            }
            else if (RGB_COLOR_CANNOT_BE_NULL.equals(expectedApiError) || NOT_RGB_COLOR_ENUM.equals(expectedApiError)) {
                extraMetadataFieldValue = "rgb_color";
            }

            if (extraMetadataFieldValue != null) {
                expectedApiError = new ApiErrorWithMetadata(
                    expectedApiError,
                    MapBuilder.builder("field", (Object) extraMetadataFieldValue).build()
                );
            }

            expectedErrors.add(expectedApiError);
        }
        verifyErrorReceived(response, expectedErrors, 400);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_MANUALLY_THROWN_ERROR_is_thrown_when_requested(ServerScenario scenario) throws IOException {
        SampleModel requestPayload = new SampleModel("bar", "42", "RED", true);
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .contentType(ContentType.JSON)
                .basePath(SAMPLE_PATH)
                .body(requestPayloadAsString)
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.MANUALLY_THROWN_ERROR);
        // This code path also should add some custom headers to the response
        assertThat(response.headers().getValues("rgbColorValue")).isEqualTo(singletonList(requestPayload.rgb_color));
        assertThat(response.headers().getValues("otherExtraMultivalueHeader")).isEqualTo(Arrays.asList("foo", "bar"));
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_SOME_MEANINGFUL_ERROR_NAME_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.SOME_MEANINGFUL_ERROR_NAME);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_GENERIC_SERVICE_ERROR_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + TRIGGER_UNHANDLED_ERROR_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.GENERIC_SERVICE_ERROR);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_WEBFLUX_MONO_ERROR_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + MONO_ERROR_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.WEBFLUX_MONO_ERROR);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_WEBFLUX_FLUX_ERROR_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + FLUX_ERROR_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.WEBFLUX_FLUX_ERROR);
    }

    // *************** FRAMEWORK/FILTER ERRORS ******************

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_NOT_FOUND_returned_if_unknown_path_is_requested(ServerScenario scenario) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(UUID.randomUUID().toString())
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NOT_FOUND);
    }

    private enum WebFilterErrorScenario {
        EXCEPTION_THROWN_IN_WEB_FILTER(
            "throw-web-filter-exception", SampleProjectApiError.ERROR_THROWN_IN_WEB_FILTER
        ),
        EXCEPTION_RETURNED_IN_WEB_FILTER(
            "return-exception-in-web-filter-mono", SampleProjectApiError.ERROR_RETURNED_IN_WEB_FILTER_MONO
        );

        public final String triggeringHeaderName;
        public final ApiError expectedError;

        WebFilterErrorScenario(String triggeringHeaderName, ApiError expectedError) {
            this.triggeringHeaderName = triggeringHeaderName;
            this.expectedError = expectedError;
        }
    }

    @DataProvider
    public static List<List<Object>> webFilterErrorScenariosDataProvider() {
        List<List<Object>> result = new ArrayList<>();
        for (WebFilterErrorScenario webFilterErrorScenario : WebFilterErrorScenario.values()) {
            for (ServerScenario serverScenario : ServerScenario.values()) {
                result.add(Arrays.asList(webFilterErrorScenario, serverScenario));
            }
        }
        return result;
    }

    @UseDataProvider("webFilterErrorScenariosDataProvider")
    @Test
    public void verify_expected_error_returned_if_web_filter_trigger_occurs(
        WebFilterErrorScenario webFilterErrorScenario, ServerScenario serverScenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(serverScenario.serverPort)
                .basePath("/doesnotmatter")
                .header(webFilterErrorScenario.triggeringHeaderName, "true")
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, webFilterErrorScenario.expectedError);
    }

    private enum RouterHandlerFilterErrorScenario {
        EXCEPTION_THROWN_IN_ROUTER_HANDLER_FILTER(
            "throw-handler-filter-function-exception",
            SampleProjectApiError.ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION
        ),
        EXCEPTION_RETURNED_IN_ROUTER_HANDLER_FILTER(
            "return-exception-in-handler-filter-function-mono",
            SampleProjectApiError.ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO
        );

        public final String triggeringHeaderName;
        public final ApiError expectedError;

        RouterHandlerFilterErrorScenario(String triggeringHeaderName, ApiError expectedError) {
            this.triggeringHeaderName = triggeringHeaderName;
            this.expectedError = expectedError;
        }
    }

    @DataProvider
    public static List<List<Object>> routerHandlerFilterErrorScenariosDataProvider() {
        List<List<Object>> result = new ArrayList<>();
        for (RouterHandlerFilterErrorScenario routerHandlerFilterErrorScenario : RouterHandlerFilterErrorScenario.values()) {
            for (ServerScenario serverScenario : ServerScenario.values()) {
                result.add(Arrays.asList(routerHandlerFilterErrorScenario, serverScenario));
            }
        }
        return result;
    }

    @UseDataProvider("routerHandlerFilterErrorScenariosDataProvider")
    @Test
    public void verify_expected_error_returned_if_handler_filter_function_trigger_occurs(
        RouterHandlerFilterErrorScenario routerHandlerFilterErrorScenario, ServerScenario serverScenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(serverScenario.serverPort)
                .basePath(SAMPLE_FROM_ROUTER_FUNCTION_PATH)
                .header(routerHandlerFilterErrorScenario.triggeringHeaderName, "true")
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, routerHandlerFilterErrorScenario.expectedError);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_METHOD_NOT_ALLOWED_returned_if_known_path_is_requested_with_invalid_http_method(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .log().all()
                .when()
                .delete()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.METHOD_NOT_ALLOWED);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_sample_get_fails_with_NO_ACCEPTABLE_REPRESENTATION_if_passed_invalid_accept_header(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .accept(ContentType.BINARY)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NO_ACCEPTABLE_REPRESENTATION);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_sample_post_fails_with_UNSUPPORTED_MEDIA_TYPE_if_passed_invalid_content_type(
        ServerScenario scenario
    ) throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .body(requestPayloadAsString)
                .contentType(ContentType.TEXT)
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.UNSUPPORTED_MEDIA_TYPE);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_MALFORMED_REQUEST_is_thrown_when_required_data_is_missing(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + WITH_REQUIRED_QUERY_PARAM_SUBPATH)
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(
            response,
            new ApiErrorWithMetadata(
                SampleCoreApiError.MALFORMED_REQUEST,
                Pair.of("missing_param_type", "int"),
                Pair.of("missing_param_name", "requiredQueryParamValue")
            )
        );
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_TYPE_CONVERSION_ERROR_is_thrown_when_framework_cannot_convert_type(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH + WITH_REQUIRED_QUERY_PARAM_SUBPATH)
                .queryParam("requiredQueryParamValue", "not-an-integer")
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, new ApiErrorWithMetadata(
            SampleCoreApiError.TYPE_CONVERSION_ERROR,
            // We can't expect the bad_property_name=requiredQueryParamValue metadata like we do in Spring Web MVC,
            //      because Spring WebFlux doesn't add it to the TypeMismatchException cause.
            MapBuilder.builder("bad_property_value", (Object) "not-an-integer")
                      .put("required_type", "int")
                      .build()
        ));
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_sample_post_fails_with_MISSING_EXPECTED_CONTENT_if_passed_empty_body(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .contentType(ContentType.JSON)
                .body("")
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.MISSING_EXPECTED_CONTENT);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_sample_post_fails_with_MALFORMED_REQUEST_if_passed_junk_json(
        ServerScenario scenario
    ) {
        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .contentType(ContentType.JSON)
                .body("{notjson blah")
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.MALFORMED_REQUEST);
    }

    @UseDataProvider("serverScenarioDataProvider")
    @Test
    public void verify_sample_post_fails_with_MALFORMED_REQUEST_if_passed_bad_json_body(
        ServerScenario scenario
    ) throws IOException {
        SampleModel originalValidPayloadObj = randomizedSampleModel();
        String originalValidPayloadAsString = objectMapper.writeValueAsString(originalValidPayloadObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> badRequestPayloadAsMap = objectMapper.readValue(originalValidPayloadAsString, Map.class);
        badRequestPayloadAsMap.put("throw_manual_error", "not-a-boolean");
        String badJsonPayloadAsString = objectMapper.writeValueAsString(badRequestPayloadAsMap);

        ExtractableResponse response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .contentType(ContentType.JSON)
                .body(badJsonPayloadAsString)
                .log().all()
                .when()
                .post()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.MALFORMED_REQUEST);
    }
}
