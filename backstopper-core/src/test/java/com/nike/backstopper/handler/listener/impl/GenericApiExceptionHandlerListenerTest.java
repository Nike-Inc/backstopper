package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link GenericApiExceptionHandlerListener}
 */
public class GenericApiExceptionHandlerListenerTest extends ListenerTestBase {

    private final GenericApiExceptionHandlerListener listener = new GenericApiExceptionHandlerListener();

    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new Exception()), false, null);
    }

    @Test
    public void shouldReturnErrorsFromApiException() {
        ApiError error = BarebonesCoreApiErrorForTesting.NOT_FOUND;
        ApiException ex = ApiException.newBuilder().withApiErrors(error).withExceptionMessage("Nice message").build();
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, singletonList(error));
    }

    @Test
    public void shouldReturnLoggingDetailsFromApiException() {
        // given
        ApiError error = BarebonesCoreApiErrorForTesting.NOT_FOUND;
        List<Pair<String, String>> extraLogInfo = Arrays.asList(Pair.of("key1", "val1"), Pair.of("key2", "val2"));
        ApiException ex = ApiException.newBuilder().withApiErrors(error).withExtraDetailsForLogging(extraLogInfo).build();
        List<Pair<String, String>> expectedExtraLogInfo = Arrays.asList(Pair.of("key1", "val1"), Pair.of("key2", "val2"), Pair.of("api_exception_message", error.getMessage()));
        
        //when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        assertThat(result.extraDetailsForLogging, is(expectedExtraLogInfo));
    }

    @Test
    public void should_add_response_headers_from_ApiException() {
        // given
        ApiError error = BarebonesCoreApiErrorForTesting.NOT_FOUND;
        List<Pair<String, List<String>>> extraHeaders = Arrays.asList(
            Pair.of("key1", singletonList("val1")),
            Pair.of("key2", Arrays.asList("val2.1", "val2.2"))
        );
        ApiException ex = ApiException.newBuilder()
                                      .withApiErrors(error)
                                      .withExtraResponseHeaders(extraHeaders)
                                      .build();

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        Assertions.assertThat(result.extraResponseHeaders).isEqualTo(extraHeaders);
    }

    @Test
    public void shouldAddExceptionMessageIfExceptionMessageIsNonEmpty() {
        ApiError error = BarebonesCoreApiErrorForTesting.NOT_FOUND;
        ApiException ex = ApiException.newBuilder().withApiErrors(error).withExceptionMessage("Nice message").build();
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        assertThat(result.extraDetailsForLogging.size(), is(1));
        assertThat(result.extraDetailsForLogging.get(0), is(Pair.of("api_exception_message", "Nice message")));
    }

    @Test
    public void shouldAddExceptionCauseIfExceptionCauseIsNonEmpty() {
        ApiError error = BarebonesCoreApiErrorForTesting.NOT_FOUND;
        ApiException ex = ApiException.newBuilder().withApiErrors(error).withExceptionCause(new Exception("intentional test exception")).build();
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        assertThat(result.extraDetailsForLogging.size(), is(3));
        assertThat(result.extraDetailsForLogging.get(1), is(Pair.of("exception_cause_class", "java.lang.Exception")));
        assertThat(result.extraDetailsForLogging.get(2), is(Pair.of("exception_cause_message", "intentional test exception")));
    }

}