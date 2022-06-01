package com.nike.backstopper.jersey1sample.componenttest;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.jersey1sample.Main;
import com.nike.backstopper.jersey1sample.error.SampleProjectApiError;
import com.nike.backstopper.jersey1sample.model.RgbColor;
import com.nike.backstopper.jersey1sample.model.SampleModel;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.internal.util.MapBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;

import static com.nike.backstopper.jersey1sample.error.SampleProjectApiError.INVALID_RANGE_VALUE;
import static com.nike.backstopper.jersey1sample.error.SampleProjectApiError.NOT_RGB_COLOR_ENUM;
import static com.nike.backstopper.jersey1sample.error.SampleProjectApiError.RGB_COLOR_CANNOT_BE_NULL;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.SAMPLE_PATH;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.TRIGGER_UNHANDLED_ERROR_SUBPATH;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.WITH_INT_QUERY_PARAM_SUBPATH;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.nextRandomColor;
import static com.nike.backstopper.jersey1sample.resource.SampleResource.nextRangeInt;
import static com.nike.internal.util.testing.TestUtils.findFreePort;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Component test that starts up the sample server and hits it with requests that should generate specific errors.
 *
 * @author Nic Munroe
 */
public class VerifyExpectedErrorsAreReturnedComponentTest {

    private static final int SERVER_PORT = findFreePort();
    private static Server server;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void beforeClass() throws Exception {
        server = Main.createServer(SERVER_PORT);
        server.start();
        for (int i = 0; i < 100; i++) {
            if (server.isStarted())
                return;
            Thread.sleep(100);
        }
        throw new IllegalStateException("Server is not up after waiting 10 seconds. Aborting tests.");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stop();
            server.destroy();
        }
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

    private SampleModel randomizedSampleModel() {
        return new SampleModel(UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false);
    }

    // *************** SUCCESSFUL (NON ERROR) CALLS ******************
    @Test
    public void verify_basic_sample_get() throws IOException {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @Test
    public void verify_basic_sample_post() throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @CsvSource(
        value = {
            "null   |   42  |   GREEN   |   FOO_STRING_CANNOT_BE_BLANK  |   400",
            "bar    |   -1  |   GREEN   |   INVALID_RANGE_VALUE         |   400",
            "bar    |   42  |   null    |   RGB_COLOR_CANNOT_BE_NULL    |   400",
            "bar    |   42  |   car     |   NOT_RGB_COLOR_ENUM          |   400",
            "       |   99  |   tree    |   FOO_STRING_CANNOT_BE_BLANK,INVALID_RANGE_VALUE,NOT_RGB_COLOR_ENUM   |   400",
        },
        delimiter = '|',
        nullValues = { "null" }
    )
    @ParameterizedTest
    public void verify_jsr303_validation_errors(
        String fooString, String rangeString, String rgbColorString,
        String expectedErrorsComboString, int expectedResponseHttpStatusCode) throws JsonProcessingException
    {
        SampleModel requestPayload = new SampleModel(fooString, rangeString, rgbColorString, false);
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .contentType(ContentType.JSON)
                .basePath(SAMPLE_PATH)
                .body(requestPayloadAsString)
                .log().all()
            .when()
                .post()
            .then()
                .log().all()
                .extract();

        String[] expectedErrorsArray = expectedErrorsComboString.split(",");
        List<ApiError> expectedErrors = new ArrayList<>();
        for (String errorStr : expectedErrorsArray) {
            ApiError apiError = SampleProjectApiError.valueOf(errorStr);
            String extraMetadataFieldValue = null;

            if (INVALID_RANGE_VALUE.equals(apiError))
                extraMetadataFieldValue = "range_0_to_42";
            else if (RGB_COLOR_CANNOT_BE_NULL.equals(apiError) || NOT_RGB_COLOR_ENUM.equals(apiError))
                extraMetadataFieldValue = "rgb_color";

            if (extraMetadataFieldValue != null)
                apiError = new ApiErrorWithMetadata(apiError, MapBuilder.builder("field", (Object)extraMetadataFieldValue).build());

            expectedErrors.add(apiError);
        }
        verifyErrorReceived(response, expectedErrors, expectedResponseHttpStatusCode);
    }

    @Test
    public void verify_sample_post_fails_with_MISSING_EXPECTED_CONTENT_if_passed_empty_body() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @Test
    public void verify_MANUALLY_THROWN_ERROR_is_thrown_when_requested() throws IOException {
        SampleModel requestPayload = new SampleModel("bar", "42", "RED", true);
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @Test
    public void verify_SOME_MEANINGFUL_ERROR_NAME_is_thrown_when_correct_endpoint_is_hit() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(SAMPLE_PATH + CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH)
                .log().all()
            .when()
                .get()
            .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleProjectApiError.SOME_MEANINGFUL_ERROR_NAME);
    }

    @Test
    public void verify_GENERIC_SERVICE_ERROR_is_thrown_when_correct_endpoint_is_hit() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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
    public void verify_METHOD_NOT_ALLOWED_returned_if_known_path_is_requested_with_invalid_http_method() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(SAMPLE_PATH)
                .log().all()
            .when()
                .delete()
            .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.METHOD_NOT_ALLOWED);
    }

    @Test
    public void verify_sample_get_fails_with_NO_ACCEPTABLE_REPRESENTATION_if_passed_invalid_accept_header() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @Test
    public void verify_sample_post_fails_with_UNSUPPORTED_MEDIA_TYPE_if_passed_invalid_content_type() throws IOException {
        SampleModel requestPayload = randomizedSampleModel();
        String requestPayloadAsString = objectMapper.writeValueAsString(requestPayload);

        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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

    @Test
    public void verify_NOT_FOUND_is_thrown_when_framework_cannot_convert_query_param_type() {
        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
                .basePath(SAMPLE_PATH + WITH_INT_QUERY_PARAM_SUBPATH)
                .queryParam("requiredQueryParamValue", "not-an-integer")
                .log().all()
            .when()
                .get()
            .then()
                .log().all()
                .extract();

        verifyErrorReceived(response, SampleCoreApiError.NOT_FOUND);
    }

    @Test
    public void verify_sample_post_fails_with_MALFORMED_REQUEST_if_passed_bad_json_body() throws IOException {
        SampleModel originalValidPayloadObj = randomizedSampleModel();
        String originalValidPayloadAsString = objectMapper.writeValueAsString(originalValidPayloadObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> badRequestPayloadAsMap = objectMapper.readValue(originalValidPayloadAsString, Map.class);
        badRequestPayloadAsMap.put("throw_manual_error", "not-a-boolean");
        String badJsonPayloadAsString = objectMapper.writeValueAsString(badRequestPayloadAsMap);

        ExtractableResponse<?> response =
            given()
                .baseUri("http://localhost")
                .port(SERVER_PORT)
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
