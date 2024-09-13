package com.nike.backstopper.handler.listener;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link ApiExceptionHandlerListenerResult}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ApiExceptionHandlerListenerResultTest {

    @Test
    public void ignoreResponse_should_return_instance_with_correct_values() {
        // when
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.ignoreResponse();

        // then
        assertThat(val.shouldHandleResponse).isFalse();
        assertThat(val.errors)
            .isNotNull()
            .isEmpty();
        assertThat(val.extraDetailsForLogging)
            .isNotNull()
            .isEmpty();
        assertThat(val.extraResponseHeaders)
            .isNotNull()
            .isEmpty();
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleResponse_one_arg_should_work_as_expected(boolean useNull) {
        // given
        SortedApiErrorSet errors = (useNull) ? null : new SortedApiErrorSet(Arrays.asList(
            BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR, BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST
        ));

        // when
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(errors);

        // then
        verifyErrors(val, errors);
    }

    private void verifyErrors(ApiExceptionHandlerListenerResult val, SortedApiErrorSet expectedErrors) {
        if (expectedErrors == null) {
            assertThat(val.errors)
                .isNotNull()
                .isEmpty();
        }
        else {
            assertThat(val.errors).isEqualTo(expectedErrors);
        }

        // verify mutability
        ApiError newError = new ApiErrorBase(UUID.randomUUID().toString(), "foo", "bar", 400);
        // and when
        val.errors.add(newError);
        // then
        assertThat(val.errors).contains(newError);
    }

    @DataProvider(value = {
        "true   |   true",
        "false  |   true",
        "true   |   false",
        "false  |   false",
    }, splitBy = "\\|")
    @Test
    public void handleResponse_two_args_should_work_as_expected(boolean useNullErrors, boolean useNullExtraLogging) {
        // given
        SortedApiErrorSet errors = (useNullErrors) ? null : new SortedApiErrorSet(Arrays.asList(
            BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR, BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST
        ));
        List<Pair<String, String>> extraDetailsForLogging = (useNullExtraLogging) ? null : Arrays.asList(
            Pair.of("extraKey1", UUID.randomUUID().toString()), Pair.of("extraKey2", UUID.randomUUID().toString())
        );


        // when
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(
            errors, extraDetailsForLogging
        );

        // then
        verifyErrors(val, errors);
        verifyExtraLogging(val, extraDetailsForLogging);
    }

    private void verifyExtraLogging(ApiExceptionHandlerListenerResult val,
                                    List<Pair<String, String>> extraDetailsForLogging) {
        if (extraDetailsForLogging == null) {
            assertThat(val.extraDetailsForLogging)
                .isNotNull()
                .isEmpty();
        }
        else {
            assertThat(val.extraDetailsForLogging).isEqualTo(extraDetailsForLogging);
        }

        // verify mutability
        Pair<String, String> newLoggingDetail = Pair.of("foo", UUID.randomUUID().toString());
        // and when
        val.extraDetailsForLogging.add(newLoggingDetail);
        // then
        assertThat(val.extraDetailsForLogging).contains(newLoggingDetail);
    }

    @DataProvider(value = {
        "true   |   true    |   true",
        "true   |   true    |   false",
        "true   |   false   |   true",
        "true   |   false   |   false",
        "false  |   true    |   true",
        "false  |   true    |   false",
        "false  |   false   |   true",
        "false  |   false   |   false",
    }, splitBy = "\\|")
    @Test
    public void handleResponse_three_args_should_work_as_expected(
        boolean useNullErrors, boolean useNullExtraLogging, boolean useNullResponseHeaders
    ) {
        // given
        SortedApiErrorSet errors = (useNullErrors) ? null : new SortedApiErrorSet(Arrays.asList(
            BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR, BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST
        ));
        List<Pair<String, String>> extraDetailsForLogging = (useNullExtraLogging) ? null : Arrays.asList(
            Pair.of("extraKey1", UUID.randomUUID().toString()), Pair.of("extraKey2", UUID.randomUUID().toString())
        );
        List<Pair<String, List<String>>> extraResponseHeaders = (useNullResponseHeaders) ? null : Arrays.asList(
            Pair.of("header1", singletonList(UUID.randomUUID().toString())),
            Pair.of("header2", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        );

        // when
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(
            errors, extraDetailsForLogging, extraResponseHeaders
        );

        // then
        verifyErrors(val, errors);
        verifyExtraLogging(val, extraDetailsForLogging);
        verifyExtraResponseHeaders(val, extraResponseHeaders);
    }

    private void verifyExtraResponseHeaders(ApiExceptionHandlerListenerResult val,
                                            List<Pair<String, List<String>>> extraResponseHeaders) {
        if (extraResponseHeaders == null) {
            assertThat(val.extraResponseHeaders)
                .isNotNull()
                .isEmpty();
        }
        else {
            assertThat(val.extraResponseHeaders).isEqualTo(extraResponseHeaders);
        }

        // verify mutability
        Pair<String, List<String>> newHeader = Pair.of("foo", singletonList(UUID.randomUUID().toString()));
        // and when
        val.extraResponseHeaders.add(newHeader);
        // then
        assertThat(val.extraResponseHeaders).contains(newHeader);
    }

}
