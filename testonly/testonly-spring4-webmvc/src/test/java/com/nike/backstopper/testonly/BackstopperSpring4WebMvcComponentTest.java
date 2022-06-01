package com.nike.backstopper.testonly;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import serverconfig.classpathscan.Spring4WebMvcClasspathScanConfig;
import serverconfig.directimport.Spring4WebMvcDirectImportConfig;
import testonly.componenttest.spring.reusable.error.SampleProjectApiError;
import testonly.componenttest.spring.reusable.jettyserver.SpringMvcJettyComponentTestServer;
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

/**
 * Component test to verify that the functionality of {@code backstopper-spring-web-mvc} works as expected in a
 * Spring 4 environment, for both classpath-scanning and direct-import Backstopper configuration use cases.
 *
 * @author Nic Munroe
 */
public class BackstopperSpring4WebMvcComponentTest {

    private static final int CLASSPATH_SCAN_SERVER_PORT = findFreePort();
    private static final int DIRECT_IMPORT_SERVER_PORT = findFreePort();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final SpringMvcJettyComponentTestServer classpathScanServer = new SpringMvcJettyComponentTestServer(
        CLASSPATH_SCAN_SERVER_PORT, Spring4WebMvcClasspathScanConfig.class
    );

    private static final SpringMvcJettyComponentTestServer directImportServer = new SpringMvcJettyComponentTestServer(
        DIRECT_IMPORT_SERVER_PORT, Spring4WebMvcDirectImportConfig.class
    );

    @BeforeAll
    public static void beforeClass() throws Exception {
        assertThat(CLASSPATH_SCAN_SERVER_PORT).isNotEqualTo(DIRECT_IMPORT_SERVER_PORT);
        classpathScanServer.startServer();
        directImportServer.startServer();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        classpathScanServer.shutdownServer();
        directImportServer.shutdownServer();
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

    // *************** SUCCESSFUL (NON ERROR) CALLS ******************
    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_basic_sample_get(ServerScenario scenario) throws IOException {
        ExtractableResponse<?> response =
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
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.foo).isNotEmpty();
        assertThat(responseBody.range_0_to_42).isNotEmpty();
        assertThat(Integer.parseInt(responseBody.range_0_to_42)).isBetween(0, 42);
        assertThat(responseBody.rgb_color).isNotEmpty();
        assertThat(RgbColor.toRgbColor(responseBody.rgb_color)).isNotNull();
        assertThat(responseBody.throw_manual_error).isFalse();
    }

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_basic_sample_post(ServerScenario scenario) throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
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

    public static List<Object[]> jsr303ValidationErrorScenariosDataProvider() {
        List<Object[]> result = new ArrayList<>();
        for (Jsr303SampleModelValidationScenario violationScenario : Jsr303SampleModelValidationScenario.values()) {
            for (ServerScenario serverScenario : ServerScenario.values()) {
                result.add(new Object[]{violationScenario, serverScenario});
            }
        }
        return result;
    }

    @MethodSource("jsr303ValidationErrorScenariosDataProvider")
    @ParameterizedTest
    public void verify_jsr303_validation_errors(
        Jsr303SampleModelValidationScenario violationScenario, ServerScenario serverScenario
    ) throws JsonProcessingException {
        String requestPayloadAsString = objectMapper.writeValueAsString(violationScenario.model);

        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_MANUALLY_THROWN_ERROR_is_thrown_when_requested(ServerScenario scenario) throws IOException {
        SampleModel requestPayload = new SampleModel("bar", "42", "RED", true);
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_SOME_MEANINGFUL_ERROR_NAME_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_GENERIC_SERVICE_ERROR_is_thrown_when_correct_endpoint_is_hit(ServerScenario scenario) {
        ExtractableResponse<?> response =
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

    // *************** FRAMEWORK ERRORS ******************

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_NOT_FOUND_returned_if_unknown_path_is_requested(ServerScenario scenario) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING_returned_if_servlet_filter_trigger_occurs(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(scenario.serverPort)
                .basePath(SAMPLE_PATH)
                .header("throw-servlet-filter-exception", "true")
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING);
    }

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_METHOD_NOT_ALLOWED_returned_if_known_path_is_requested_with_invalid_http_method(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_sample_get_fails_with_NO_ACCEPTABLE_REPRESENTATION_if_passed_invalid_accept_header(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_sample_post_fails_with_UNSUPPORTED_MEDIA_TYPE_if_passed_invalid_content_type(
        ServerScenario scenario
    ) throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_MALFORMED_REQUEST_is_thrown_when_required_data_is_missing(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_TYPE_CONVERSION_ERROR_is_thrown_when_framework_cannot_convert_type(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
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
            MapBuilder.builder("bad_property_name", (Object)"requiredQueryParamValue")
                      .put("bad_property_value", "not-an-integer")
                      .put("required_type", "int")
                      .build()
        ));
    }

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_sample_post_fails_with_MISSING_EXPECTED_CONTENT_if_passed_empty_body(
        ServerScenario scenario
    ) {
        ExtractableResponse<?> response =
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

    @EnumSource(ServerScenario.class)
    @ParameterizedTest
    public void verify_sample_post_fails_with_MALFORMED_REQUEST_if_passed_bad_json_body(
        ServerScenario scenario
    ) throws IOException {
        SampleModel originalValidPayloadObj = randomizedSampleModel();
        String originalValidPayloadAsString = objectMapper.writeValueAsString(originalValidPayloadObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> badRequestPayloadAsMap = objectMapper.readValue(originalValidPayloadAsString, Map.class);
        badRequestPayloadAsMap.put("throw_manual_error", "not-a-boolean");
        String badJsonPayloadAsString = objectMapper.writeValueAsString(badRequestPayloadAsMap);

        ExtractableResponse<?> response =
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
