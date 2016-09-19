package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.ApiErrorConstants;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.network.ServerHttpStatusCodeException;
import com.nike.backstopper.exception.network.ServerTimeoutException;
import com.nike.backstopper.exception.network.ServerUnknownHttpStatusCodeException;
import com.nike.backstopper.exception.network.ServerUnreachableException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link DownstreamNetworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
public class DownstreamNetworkExceptionHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private DownstreamNetworkExceptionHandlerListener listener = new DownstreamNetworkExceptionHandlerListener(testProjectApiErrors);

    private static class HttpClientErrorExceptionForTests extends Exception {
        public final int statusCode;
        public final Map<String, List<String>> headers;
        public final String rawResponseBody;

        public HttpClientErrorExceptionForTests(int statusCode, Map<String, List<String>> headers, String rawResponseBody) {
            super();
            this.statusCode = statusCode;
            this.headers = headers;
            this.rawResponseBody = rawResponseBody;
        }
    }

    @Test
    public void constructor_sets_projectApiErrors_to_passed_in_arg() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);

        // when
        DownstreamNetworkExceptionHandlerListener impl = new DownstreamNetworkExceptionHandlerListener(projectErrorsMock);

        // then
        Assertions.assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new DownstreamNetworkExceptionHandlerListener(null);
            }
        });

        // then
        Assertions.assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new ApiException(testProjectApiErrors.getGenericServiceError())), false, null);
    }

    @Test
    public void shouldReturnTEMPORARY_SERVICE_PROBLEMForServerTimeoutException() {
        ServerTimeoutException ex = new ServerTimeoutException(new Exception(), "FOO");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getTemporaryServiceProblemApiError()));
    }

    @Test
    public void shouldReturnTEMPORARY_SERVICE_PROBLEMForServerUnreachableException() {
        ServerUnreachableException ex = new ServerUnreachableException(new Exception(), "FOO");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getTemporaryServiceProblemApiError()));
    }

    @Test
    public void shouldReturnTEMPORARY_SERVICE_PROBLEMForTimeoutException() {
        TimeoutException ex = new TimeoutException("intentional test exception");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getTemporaryServiceProblemApiError()));
    }

    @Test
    public void shouldReturnTEMPORARY_SERVICE_PROBLEMForConnectException() {
        ConnectException ex = new ConnectException("intentional test exception");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getTemporaryServiceProblemApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNullDetails() {
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), null, null, null, null, null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
        // Should also include status code and raw response info in the extra logging details showing that everything was null
        assertThat(result.extraDetailsForLogging.contains(Pair.of("status_code", "null")), is(true));
        assertThat(result.extraDetailsForLogging.contains(Pair.of("raw_response_string", (String) null)), is(true));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNonNullDetailsAndNullConnType() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(400, null, null);
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), null, serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNonNullDetailsAndNonNullConnTypeAndEmptyResponseBodyText() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(400, null, " ");
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNonNullDetailsAndNonNullConnTypeAndBrokenResponseBodyText() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(400, null, "{notvalidjson");
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNonNullDetailsAndNonNullConnTypeAndValidResponseBodyTextWithoutErrorCode() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(400, null, "{\"somekey\":\"someval\"}");
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerHttpStatusCodeExceptionWithNonNullDetailsAndNonNullConnTypeAndValidResponseBodyTextWithErrorCode() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(400, null, "{\"errorCode\":\"123\"}");
        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
    }

    @Test
    public void processServerHttpStatusCodeExceptionShouldIncludeStatusCodeAndRawResponseString() {
        String rawResponseString = String.format("{\"%s\":\"blah\"}", UUID.randomUUID());
        HttpClientErrorExceptionForTests details = new HttpClientErrorExceptionForTests(500, null, rawResponseString);

        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", details, details.statusCode, details.headers, details.rawResponseBody);
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        listener.processServerHttpStatusCodeException(ex, extraDetailsForLogging);

        assertThat(extraDetailsForLogging.contains(Pair.of("raw_response_string", rawResponseString)), is(true));
        assertThat(extraDetailsForLogging.contains(Pair.of("status_code", String.valueOf(details.statusCode))), is(true));
    }

    @Test
    public void processServerHttpStatusCodeExceptionShouldReturnOUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERRORWhenItSeesTheCorrectStatusCode() throws Exception {
        HttpClientErrorExceptionForTests details = new HttpClientErrorExceptionForTests(ApiErrorConstants.HTTP_STATUS_CODE_SERVICE_UNAVAILABLE, null, null);

        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", details, details.statusCode, details.headers, details.rawResponseBody);
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        SortedApiErrorSet errors = listener.processServerHttpStatusCodeException(ex, extraDetailsForLogging);

        assertThat(errors.size(), is(1));
        assertThat(errors.first(), is(testProjectApiErrors.getOutsideDependencyReturnedTemporaryErrorApiError()));
    }

    @Test
    public void processServerHttpStatusCodeExceptionShouldReturnTOO_MANY_REQUESTSWhenItSeesTheCorrectStatusCode() throws Exception {
        HttpClientErrorExceptionForTests details = new HttpClientErrorExceptionForTests(ApiErrorConstants.HTTP_STATUS_CODE_TOO_MANY_REQUESTS, null, null);

        ServerHttpStatusCodeException ex = new ServerHttpStatusCodeException(new Exception(), "FOO", details, details.statusCode, details.headers, details.rawResponseBody);
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        SortedApiErrorSet errors = listener.processServerHttpStatusCodeException(ex, extraDetailsForLogging);

        assertThat(errors.size(), is(1));
        assertThat(errors.first(), is(testProjectApiErrors.getTooManyRequestsApiError()));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerUnknownHttpStatusCodeExceptionWitNullDetails() {
        ServerUnknownHttpStatusCodeException ex = new ServerUnknownHttpStatusCodeException(new Exception(), "FOO", null, null, null, null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
        // Should also include status code and raw response info in the extra logging details showing that everything was null
        assertThat(result.extraDetailsForLogging.contains(Pair.of("status_code", "null")), is(true));
        assertThat(result.extraDetailsForLogging.contains(Pair.of("raw_response_string", "\"null\"")), is(true));
    }

    @Test
    public void shouldReturnOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERRORForServerUnknownHttpStatusCodeExceptionWitNonNullDetails() {
        HttpClientErrorExceptionForTests serverResponseEx = new HttpClientErrorExceptionForTests(-1, null, "foo");
        ServerUnknownHttpStatusCodeException ex = new ServerUnknownHttpStatusCodeException(new Exception(), "FOO", serverResponseEx, serverResponseEx.statusCode, serverResponseEx.headers, serverResponseEx.rawResponseBody);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError()));
        // Should also include status code and raw response info in the extra logging details showing what was returned in the response
        assertThat(result.extraDetailsForLogging.contains(Pair.of("status_code", "-1")), is(true));
        assertThat(result.extraDetailsForLogging.contains(Pair.of("raw_response_string", "\"foo\"")), is(true));
    }
}
