package com.nike.backstopper.exception;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link com.nike.backstopper.exception.ApiException} and its builder
 * {@link com.nike.backstopper.exception.ApiException.Builder}
 */
@RunWith(DataProviderRunner.class)
public class ApiExceptionTest {

    private ApiError apiError1 = BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
    private ApiError apiError2 = BarebonesCoreApiErrorForTesting.SERVERSIDE_VALIDATION_ERROR;
    private ApiError apiError3 = BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR;
    private ApiError apiError4 = BarebonesCoreApiErrorForTesting.OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR;

    private Pair<String, String> logPair1 = Pair.of("key1", "val1");
    private Pair<String, String> logPair2 = Pair.of("key2", "val2");
    private Pair<String, String> logPair3 = Pair.of("key3", "val3");
    private Pair<String, String> logPair4 = Pair.of("key4", "val4");

    private Pair<String, List<String>> headerPair1 = Pair.of("h1", singletonList("v1"));
    private Pair<String, List<String>> headerPair2 = Pair.of("h2", Arrays.asList("v2.1", "v2.2"));
    private Pair<String, List<String>> headerPair3 = Pair.of("h3", singletonList("v3"));
    private Pair<String, List<String>> headerPair4 = Pair.of("h4", Arrays.asList("v4.1", "v4.2"));

    private String exceptionMessage = "some ex msg";
    private Exception cause = new Exception("intentional test exception");

    @DataProvider(value = {
        "true   |   FORCE_STACK_TRACE",
        "true   |   FORCE_NO_STACK_TRACE",
        "true   |   DEFER_TO_DEFAULT_BEHAVIOR",
        "true   |   null",
        "false  |   FORCE_STACK_TRACE",
        "false  |   FORCE_NO_STACK_TRACE",
        "false  |   DEFER_TO_DEFAULT_BEHAVIOR",
        "false  |   null",
    }, splitBy = "\\|")
    @Test
    public void builder_dot_build_and_constructor_that_takes_builder_arg_are_identical_and_work_as_expected(
        boolean includeCause,
        StackTraceLoggingBehavior desiredStackTraceLoggingBehavior
    ) {
        // given
        ApiException.Builder builder = ApiException.newBuilder()
                .withApiErrors(Arrays.asList(apiError1, apiError2))
                .withApiErrors(apiError3, apiError4)
                .withExtraDetailsForLogging(Arrays.asList(logPair1, logPair2))
                .withExtraDetailsForLogging(logPair3, logPair4)
                .withExtraResponseHeaders(Arrays.asList(headerPair1, headerPair2))
                .withExtraResponseHeaders(headerPair3, headerPair4)
                .withExceptionMessage(exceptionMessage)
                .withStackTraceLoggingBehavior(desiredStackTraceLoggingBehavior);

        if (includeCause)
            builder.withExceptionCause(cause);

        StackTraceLoggingBehavior expectedStackTraceLoggingBehavior =
            (desiredStackTraceLoggingBehavior == null)
            ? StackTraceLoggingBehavior.DEFER_TO_DEFAULT_BEHAVIOR
            : desiredStackTraceLoggingBehavior;

        // when
        ApiException apiExceptionFromBuilder = builder.build();
        ApiException apiExceptionFromConstructor = new ApiException(builder);

        // then
        assertThat(apiExceptionFromBuilder.getApiErrors())
            .isEqualTo(apiExceptionFromConstructor.getApiErrors())
            .isEqualTo(Arrays.asList(apiError1, apiError2, apiError3, apiError4));

        assertThat(apiExceptionFromBuilder.getExtraDetailsForLogging())
            .isEqualTo(apiExceptionFromConstructor.getExtraDetailsForLogging())
            .isEqualTo(Arrays.asList(logPair1, logPair2, logPair3, logPair4));

        assertThat(apiExceptionFromBuilder.getExtraResponseHeaders())
            .isEqualTo(apiExceptionFromConstructor.getExtraResponseHeaders())
            .isEqualTo(Arrays.asList(headerPair1, headerPair2, headerPair3, headerPair4));

        assertThat(apiExceptionFromBuilder.getMessage())
            .isEqualTo(apiExceptionFromConstructor.getMessage())
            .isEqualTo(exceptionMessage);

        assertThat(apiExceptionFromBuilder.getStackTraceLoggingBehavior())
            .isEqualTo(apiExceptionFromConstructor.getStackTraceLoggingBehavior())
            .isEqualTo(expectedStackTraceLoggingBehavior);

        if (includeCause) {
            assertThat(apiExceptionFromBuilder.getCause())
                .isSameAs(apiExceptionFromConstructor.getCause())
                .isSameAs(cause);
        }
        else {
            assertThat(apiExceptionFromBuilder.getCause())
                .isEqualTo(apiExceptionFromConstructor.getCause())
                .isNull();
        }
    }

    @Test
    public void constructor_with_builder_arg_uses_expected_defaults_when_optional_builder_fields_are_null() {
        // given
        List<ApiError> apiErrors = Arrays.asList(apiError1, apiError2);

        ApiException.Builder builder = ApiException
            .newBuilder()
            .withApiErrors(apiErrors)
            .withStackTraceLoggingBehavior(null)
            .withExceptionMessage(null)
            .withExceptionCause(null);

        String expectedMessage = ApiException.extractMessage(apiErrors, null);
        StackTraceLoggingBehavior expectedStackTraceLoggingBehavior =
            StackTraceLoggingBehavior.DEFER_TO_DEFAULT_BEHAVIOR;

        // when
        ApiException apiException = new ApiException(builder);

        // then
        assertThat(apiException.getApiErrors()).isEqualTo(apiErrors);
        assertThat(apiException.getStackTraceLoggingBehavior()).isEqualTo(expectedStackTraceLoggingBehavior);
        assertThat(apiException.getMessage()).isEqualTo(expectedMessage);
        assertThat(apiException.getCause()).isNull();
    }

    @Test
    public void constructor_with_builder_arg_throws_IllegalArgumentException_when_passed_empty_ApiError_list() {
        // given
        ApiException.Builder builder = ApiException.newBuilder();

        // when
        Throwable ex = catchThrowable(() -> new ApiException(builder));

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The Builder's apiErrors cannot be empty");
    }

    @Test(expected = IllegalArgumentException.class)
    public void single_error_constructor_fails_if_passed_null_arg() {
        // expect
        new ApiException((ApiError)null);
    }

    @Test
    public void single_error_constructor_sets_expected_values() {
        // given
        ApiException apiException = new ApiException(GENERIC_SERVICE_ERROR);

        // then
        assertThat(apiException.getMessage()).isEqualTo(GENERIC_SERVICE_ERROR.getMessage());
        assertThat(apiException.getApiErrors().size()).isEqualTo(1);
        assertThat(apiException.getApiErrors().contains(GENERIC_SERVICE_ERROR)).isTrue();
        assertThat(apiException.getExtraDetailsForLogging().isEmpty()).isTrue();
        assertThat(apiException.getExtraResponseHeaders().isEmpty()).isTrue();
    }

    @DataProvider(value = {
        "true   |   true",
        "true   |   false",
        "false  |   true",
        "false  |   false",
    }, splitBy = "\\|")
    @Test(expected = IllegalArgumentException.class)
    public void no_cause_constructors_fail_when_passed_null_or_empty_apiErrors_list(
        boolean useNull, boolean useConstructorWithResponseHeaders
    ) {
        // given
        List<Pair<String, String>> logInfoList = Collections.emptyList();
        List<Pair<String, List<String>>> responseHeaders = Collections.emptyList();
        List<ApiError> apiErrors = (useNull) ? null : Collections.<ApiError>emptyList();

        // expect
        if (useConstructorWithResponseHeaders)
            new ApiException(apiErrors, logInfoList, responseHeaders, exceptionMessage);
        else
            new ApiException(apiErrors, logInfoList, exceptionMessage);
    }

    @DataProvider(value = {
        "true   |   true",
        "true   |   false",
        "false  |   true",
        "false  |   false",
    }, splitBy = "\\|")
    @Test(expected = IllegalArgumentException.class)
    public void with_cause_constructors_fail_when_passed_null_or_empty_apiErrors_list(
        boolean useNull, boolean useConstructorWithResponseHeaders
    ) {
        // given
        List<Pair<String, String>> logInfoList = Collections.emptyList();
        List<Pair<String, List<String>>> responseHeaders = Collections.emptyList();
        List<ApiError> apiErrors = (useNull) ? null : Collections.<ApiError>emptyList();

        // expect
        if (useConstructorWithResponseHeaders)
            new ApiException(apiErrors, logInfoList, responseHeaders, exceptionMessage, cause);
        else
            new ApiException(apiErrors, logInfoList, exceptionMessage, cause);
    }

    @DataProvider(value = {
        "true",
        "false"
    }, splitBy = "\\|")
    @Test
    public void no_cause_constructors_should_translate_null_logging_details_to_empty_list(
        boolean useConstructorWithResponseHeaders
    ) {
        // given
        List<Pair<String, List<String>>> responseHeaders = Collections.emptyList();
        List<ApiError> apiErrors = Arrays.asList(apiError1, apiError2);

        // when
        ApiException apiException = (useConstructorWithResponseHeaders)
                                    ? new ApiException(apiErrors, null, exceptionMessage)
                                    : new ApiException(apiErrors, null, responseHeaders, exceptionMessage);

        // then
        assertThat(apiException.getExtraDetailsForLogging())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void no_cause_constructor_should_translate_null_response_headers_to_empty_list() {
        // given
        List<Pair<String, String>> loggingDetails = Collections.emptyList();
        List<ApiError> apiErrors = Arrays.asList(apiError1, apiError2);

        // when
        ApiException apiException = new ApiException(apiErrors, loggingDetails, null, exceptionMessage);

        // then
        assertThat(apiException.getExtraResponseHeaders())
            .isNotNull()
            .isEmpty();
    }

    @DataProvider(value = {
        "true",
        "false"
    }, splitBy = "\\|")
    @Test
    public void with_cause_constructors_should_translate_null_logging_details_to_empty_list(
        boolean useConstructorWithResponseHeaders
    ) {
        // given
        List<Pair<String, List<String>>> responseHeaders = Collections.emptyList();
        List<ApiError> apiErrors = Arrays.asList(apiError1, apiError2);

        // when
        ApiException apiException = (useConstructorWithResponseHeaders)
                                    ? new ApiException(apiErrors, null, exceptionMessage, cause)
                                    : new ApiException(apiErrors, null, responseHeaders, exceptionMessage, cause);

        // then
        assertThat(apiException.getExtraDetailsForLogging())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void with_cause_constructor_should_translate_null_response_headers_to_empty_list() {
        // given
        List<Pair<String, String>> loggingDetails = Collections.emptyList();
        List<ApiError> apiErrors = Arrays.asList(apiError1, apiError2);

        // when
        ApiException apiException = new ApiException(apiErrors, loggingDetails, null, exceptionMessage, cause);

        // then
        assertThat(apiException.getExtraResponseHeaders())
            .isNotNull()
            .isEmpty();
    }

    @Test
    public void extractMessage_listOfErrors_with_message_returns_message() {
        // given
        String desiredMessage = "desiredMessage";

        // when
        String extractedMessage = ApiException.extractMessage(null, desiredMessage);

        // then
        assertThat(extractedMessage).isEqualTo(desiredMessage);
    }

    @Test
    public void extractMessage_nullListOfErrors_without_message_returns_null() {
        // when
        String extractedMessage = ApiException.extractMessage(null, null);

        // then
        assertThat(extractedMessage).isNull();
    }

    @Test
    public void extractMessage_listOf1Error_without_message_returns_message() {
        // given
        List<ApiError> apiErrors = singletonList(apiError1);

        // when
        String extractedMessage = ApiException.extractMessage(apiErrors, null);

        // then
        assertThat(extractedMessage).isEqualTo(apiError1.getMessage());
    }

    @Test
    public void extractMessage_listOfErrors_without_message_returns_joined_message() {
        // given
        List<ApiError> apiErrors = Lists.newArrayList(apiError1, apiError2, apiError3);

        // when
        String extractedMessage = ApiException.extractMessage(apiErrors, null);

        // then
        String expectedMessage = "[" + apiError1.getMessage() + ", " + apiError2.getMessage() + ", " + apiError3.getMessage() + "]";
        assertThat(extractedMessage).isEqualTo(expectedMessage);
    }
}