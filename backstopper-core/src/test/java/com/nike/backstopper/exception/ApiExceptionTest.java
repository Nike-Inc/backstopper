package com.nike.backstopper.exception;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.internal.util.Pair;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the functionality of {@link com.nike.backstopper.exception.ApiException} and its builder
 * {@link com.nike.backstopper.exception.ApiException.Builder}
 */
public class ApiExceptionTest {

    private ApiError apiError1 = BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
    private ApiError apiError2 = BarebonesCoreApiErrorForTesting.SERVERSIDE_VALIDATION_ERROR;
    private ApiError apiError3 = BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR;
    private ApiError apiError4 = BarebonesCoreApiErrorForTesting.OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR;

    private Pair<String, String> logPair1 = Pair.of("key1", "val1");
    private Pair<String, String> logPair2 = Pair.of("key2", "val2");
    private Pair<String, String> logPair3 = Pair.of("key3", "val3");
    private Pair<String, String> logPair4 = Pair.of("key4", "val4");

    private String exceptionMessage = "some ex msg";
    private Exception cause = new Exception("intentional test exception");

    @Test
    public void builderWorksWithoutCause() {
        //noinspection unchecked
        ApiException apiException = ApiException.newBuilder()
                .withApiErrors(Arrays.asList(apiError1, apiError2))
                .withApiErrors(apiError3, apiError4)
                .withExtraDetailsForLogging(Arrays.asList(logPair1, logPair2))
                .withExtraDetailsForLogging(logPair3, logPair4)
                .withExceptionMessage(exceptionMessage)
                .build();

        assertThat(apiException.getApiErrors(), is(Arrays.asList(apiError1, apiError2, apiError3, apiError4)));
        assertThat(apiException.getExtraDetailsForLogging(), is(Arrays.asList(logPair1, logPair2, logPair3, logPair4)));
        assertThat(apiException.getMessage(), is(exceptionMessage));
        assertThat(apiException.getCause(), nullValue());
    }

    @Test
    public void builderWorksWithCause() {
        //noinspection unchecked
        ApiException apiException = ApiException.newBuilder()
                .withApiErrors(Arrays.asList(apiError1, apiError2))
                .withApiErrors(apiError3, apiError4)
                .withExtraDetailsForLogging(Arrays.asList(logPair1, logPair2))
                .withExtraDetailsForLogging(logPair3, logPair4)
                .withExceptionMessage(exceptionMessage)
                .withExceptionCause(cause)
                .build();

        assertThat(apiException.getApiErrors(), is(Arrays.asList(apiError1, apiError2, apiError3, apiError4)));
        assertThat(apiException.getExtraDetailsForLogging(), is(Arrays.asList(logPair1, logPair2, logPair3, logPair4)));
        assertThat(apiException.getMessage(), is(exceptionMessage));
        assertThat(apiException.getCause(), is((Throwable)cause));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noCauseConstructorShouldFailWithNullApiErrorsCollection() {
        List<Pair<String, String>> logInfoList = new ArrayList<>();
        new ApiException(null, logInfoList, exceptionMessage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noCauseConstructorShouldFailWithEmptyApiErrorsCollection() {
        List<Pair<String, String>> logInfoList = new ArrayList<>();
        new ApiException(Collections.<ApiError>emptyList(), logInfoList, exceptionMessage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withCauseConstructorShouldFailWithNullApiErrorsCollection() {
        List<Pair<String, String>> logInfoList = new ArrayList<>();
        new ApiException(null, logInfoList, exceptionMessage, cause);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withCauseConstructorShouldFailWithEmptyApiErrorsCollection() {
        List<Pair<String, String>> logInfoList = new ArrayList<>();
        new ApiException(Collections.<ApiError>emptyList(), logInfoList, exceptionMessage, cause);
    }

    @Test
    public void noCauseConstructorShouldTranslateNullLoggingDetailsToEmptyList() {
        ApiException apiException = new ApiException(Arrays.asList(apiError1, apiError2), null, exceptionMessage);
        assertThat(apiException.getExtraDetailsForLogging(), notNullValue());
        assertThat(apiException.getExtraDetailsForLogging().isEmpty(), is(true));
    }

    @Test
    public void withCauseConstructorShouldTranslateNullLoggingDetailsToEmptyList() {
        ApiException apiException = new ApiException(Arrays.asList(apiError1, apiError2), null, exceptionMessage, cause);
        assertThat(apiException.getExtraDetailsForLogging(), notNullValue());
        assertThat(apiException.getExtraDetailsForLogging().isEmpty(), is(true));
    }

    @Test
    public void builder_works_as_expected() {
        // given
        ApiError error1 = BarebonesCoreApiErrorForTesting.FORBIDDEN;
        ApiError error2 = BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST;
        ApiError error3 = BarebonesCoreApiErrorForTesting.UNSUPPORTED_MEDIA_TYPE;
        ApiError error4 = BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT;
        Pair<String, String> pair1 = Pair.of("foo", UUID.randomUUID().toString());
        Pair<String, String> pair2 = Pair.of("bar", UUID.randomUUID().toString());
        Pair<String, String> pair3 = Pair.of("baz", UUID.randomUUID().toString());
        Pair<String, String> pair4 = Pair.of("bat", UUID.randomUUID().toString());
        String exceptionMessage = UUID.randomUUID().toString();
        Throwable cause = new RuntimeException("kaboom");

        // when
        ApiException ex = ApiException.newBuilder()
                    .withApiErrors(error1, error2)
                    .withApiErrors(Arrays.asList(error3, error4))
                    .withExtraDetailsForLogging(pair1, pair2)
                    .withExtraDetailsForLogging(Arrays.asList(pair3, pair4))
                    .withExceptionMessage(exceptionMessage)
                    .withExceptionCause(cause)
                    .build();

        // then
        Assertions.assertThat(ex.getApiErrors()).isEqualTo(Arrays.asList(error1, error2, error3, error4));
        Assertions.assertThat(ex.getExtraDetailsForLogging()).isEqualTo(Arrays.asList(pair1, pair2, pair3, pair4));
        Assertions.assertThat(ex.getMessage()).isEqualTo(exceptionMessage);
        Assertions.assertThat(ex.getCause()).isEqualTo(cause);
    }
}