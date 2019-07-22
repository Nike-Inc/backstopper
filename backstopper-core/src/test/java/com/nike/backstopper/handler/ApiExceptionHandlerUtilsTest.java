package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;
import com.nike.internal.util.StringUtils;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static com.nike.backstopper.handler.ApiExceptionHandlerUtils.DEFAULT_IMPL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link com.nike.backstopper.handler.ApiExceptionHandlerUtils}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ApiExceptionHandlerUtilsTest {

    @Mock
    private RequestInfoForLogging reqMock;

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private ApiExceptionHandlerUtils impl = DEFAULT_IMPL;
    
    @Before
    public void setupMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void default_impl_has_expected_default_properties_set() {
        // expect
        assertThat(DEFAULT_IMPL.maskSensitiveHeaders, is(true));
        assertThat(DEFAULT_IMPL.sensitiveHeaderKeysForMasking, is(ApiExceptionHandlerUtils.DEFAULT_MASKED_HEADER_KEYS));
        assertThat(DEFAULT_IMPL.distributedTraceIdHeaderKey,
                   is(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY));
    }

    @Test
    public void addBaseExceptionMessageToExtraDetailsForLoggingShouldDoWhatItSays() {
        Exception ex = new Exception("some base message");
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        impl.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
        assertThat(extraDetailsForLogging.size(), is(1));
        assertThat(extraDetailsForLogging.get(0), is(Pair.of("exception_message", "some base message")));
    }

    @Test
    public void quotesToApostrophesShouldReturnNullIfYouPassItNull() {
        assertThat(impl.quotesToApostrophes(null), nullValue());
    }

    @Test
    public void quotesToApostrophesShouldDoNothingToStringWithoutQuotes() {
        String rawString = "ihavenoquotes";
        assertThat(impl.quotesToApostrophes(rawString), is(rawString));
    }

    @Test
    public void quotesToApostrophesShouldReturnStringWithQuotesReplacedWithApostrophes() {
        String rawString = "hereisa\"quote\"";
        assertThat(impl.quotesToApostrophes(rawString), is("hereisa'quote'"));
    }

    @Test
    public void extractDistributedTraceIdShouldExtractFromHeaderIfAvailable() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn("aDTraceIdFromHeader");
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn("aDTraceIdFromAttribute");

        assertThat(impl.extractDistributedTraceId(reqMock), is("aDTraceIdFromHeader"));
    }

    @Test
    public void extractDistributedTraceIdShouldExtractFromAttributeIfHeaderNotAvailable() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn("aDTraceIdFromAttribute");

        assertThat(impl.extractDistributedTraceId(reqMock), is("aDTraceIdFromAttribute"));
    }

    @Test
    public void extractDistributedTraceIdShouldReturnNullIfNotAvailableInHeaderOrAttribute() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);

        assertThat(impl.extractDistributedTraceId(reqMock), nullValue());
    }

    @Test
    public void extractDistributedTraceIdShouldReturnNullIfAttributeToStringIsNull() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);
        Object weirdObj = mock(Object.class);
        given(weirdObj.toString()).willReturn(null);
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(weirdObj);

        assertThat(impl.extractDistributedTraceId(reqMock), nullValue());
    }

    @Test
    public void extractDistributedTraceIdShouldReturnNullIfAttributeToStringIsWhitespace() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(" ");

        assertThat(impl.extractDistributedTraceId(reqMock), nullValue());
    }

    @Test
    public void extractDistributedTraceIdShouldReturnTraceFromMdcIfNotAvailableInRequest() {
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn(null);

        try {
            String expectedTraceId = UUID.randomUUID().toString();
            MDC.put(ApiExceptionHandlerUtils.TRACE_ID_MDC_KEY, expectedTraceId);
            assertThat(impl.extractDistributedTraceId(reqMock), is(expectedTraceId));
        }
        finally {
            MDC.remove(ApiExceptionHandlerUtils.TRACE_ID_MDC_KEY);
        }
    }

    private void verifyBuildErrorMessageForLogs(boolean requestHasDtraceId, String dtraceIdToUse, List<Pair<String, String>> extraDetailsForLogging) {
        String requestUri = "/some/request/uri";
        String requestMethod = "GET";
        String queryString = "param1=val1&param2=val2";
        List<FakeHeader> headers = new ArrayList<>(Arrays.asList(new FakeHeader("header1", "h1val"), new FakeHeader("header2", "h2val1", "h2val2")));
        if (requestHasDtraceId)
            headers.add(new FakeHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY, dtraceIdToUse));

        Map<String, List<String>> headersMap = new TreeMap<>();
        for (FakeHeader fh : headers) {
            headersMap.put(fh.headerName, fh.headerValues);
        }

        when(reqMock.getRequestUri()).thenReturn(requestUri);
        when(reqMock.getRequestHttpMethod()).thenReturn(requestMethod);
        when(reqMock.getQueryString()).thenReturn(queryString);
        when(reqMock.getHeadersMap()).thenReturn(headersMap);
        for (FakeHeader fh : headers) {
            when(reqMock.getHeaders(fh.headerName)).thenReturn(fh.headerValues);
            when(reqMock.getHeader(fh.headerName)).thenReturn(fh.headerValues.get(0));
        }

        List<ApiError> contributingErrors = Arrays.<ApiError>asList(BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR, BarebonesCoreApiErrorForTesting.GENERIC_BAD_REQUEST);
        int httpStatusCodeToUse = testProjectApiErrors.determineHighestPriorityHttpStatusCode(contributingErrors);
        Exception exceptionCause = new Exception();

        // Verify it without extra logging details
        StringBuilder fromHandler = new StringBuilder();
        String uid = impl.buildErrorMessageForLogs(fromHandler, reqMock, contributingErrors, httpStatusCodeToUse, exceptionCause, extraDetailsForLogging);

        // Make sure that it used a valid UUID for the error ID.
        UUID uuidFromUid = UUID.fromString(uid);
        assertThat(uuidFromUid, notNullValue());

        // Account for empty DTrace IDs
        String expectedDtraceId = (requestHasDtraceId && StringUtils.isNotBlank(dtraceIdToUse)) ? dtraceIdToUse : null;

        StringBuilder expected = new StringBuilder();
        expected.append("error_uid=").append(uid)
                .append(", dtrace_id=").append(expectedDtraceId)
                .append(", exception_class=").append(exceptionCause.getClass().getName())
                .append(", returned_http_status_code=").append(httpStatusCodeToUse)
                .append(", contributing_errors=\"").append("GENERIC_SERVICE_ERROR,GENERIC_BAD_REQUEST")
                .append("\", request_uri=\"").append(requestUri)
                .append("\", request_method=\"").append(requestMethod)
                .append("\", query_string=\"").append(queryString)
                .append("\", request_headers=\"");

        if (requestHasDtraceId) {
            expected.append(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY).append("=").append(dtraceIdToUse).append(",");
        }

        expected.append("header1=h1val,header2=[h2val1,h2val2]");
        expected.append('\"');

        if (extraDetailsForLogging != null) {
            for (Pair<String, String> extraLogDetail : extraDetailsForLogging) {
                expected.append(", ").append(extraLogDetail.getLeft()).append("=\"").append(extraLogDetail.getRight()).append('\"');
            }
        }

        assertThat(fromHandler.toString(), is(expected.toString()));
    }

    @Test
    public void buildErrorMessageForLogsReturnsExpectedStringForMissingDtraceId() {
        verifyBuildErrorMessageForLogs(false, null, null);
        verifyBuildErrorMessageForLogs(false, null, Arrays.asList(Pair.of("foo", "bar")));
    }

    @Test
    public void buildErrorMessageForLogsReturnsExpectedStringForNullDtraceId() {
        verifyBuildErrorMessageForLogs(true, null, null);
        verifyBuildErrorMessageForLogs(true, null, Arrays.asList(Pair.of("foo", "bar"), Pair.of("feefi", "fofum")));
    }

    @Test
    public void buildErrorMessageForLogsReturnsExpectedStringForEmptyDtraceId() {
        verifyBuildErrorMessageForLogs(true, " ", null);
        verifyBuildErrorMessageForLogs(true, " ", Arrays.asList(Pair.of("foo", "bar"), Pair.of("feefi", "fofum"), Pair.of("stuff", "things")));
    }

    @Test
    public void buildErrorMessageForLogsReturnsExpectedStringForValidDtraceId() {
        verifyBuildErrorMessageForLogs(true, "1234645", null);
        verifyBuildErrorMessageForLogs(true, "1234645", Arrays.asList(Pair.of("foo", "bar"), Pair.of("feefi", "fofum"), Pair.of("stuff", "things"), Pair.of("boo", "whee")));
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void buildErrorMessageForLogs_includes_orig_error_request_uri_when_available_in_request_attrs(
        boolean isErrorRequestUriValueInAttrs
    ) {
        // given
        StringBuilder sb = new StringBuilder();
        RequestInfoForLogging request = mock(RequestInfoForLogging.class);

        String origErrorRequestUriValue = UUID.randomUUID().toString();
        if (isErrorRequestUriValueInAttrs) {
            doReturn(origErrorRequestUriValue).when(request).getAttribute("javax.servlet.error.request_uri");
        }

        // when
        impl.buildErrorMessageForLogs(
            sb,
            request,
            Collections.emptyList(),
            400,
            new RuntimeException("intentional test exception"), Collections.emptyList()
        );

        // then
        String result = sb.toString();
        if (isErrorRequestUriValueInAttrs) {
            Assertions.assertThat(result)
                      .contains(String.format("orig_error_request_uri=\"%s\"", origErrorRequestUriValue));
        }
        else {
            Assertions.assertThat(result).doesNotContain("orig_error_request_uri");
        }
    }

    @DataProvider(value = {
        "foo    |   null    |   foo",
        "null   |   bar     |   bar",
        "foo    |   bar     |   foo",
        "null   |   null    |   null",
    }, splitBy = "\\|")
    @Test
    public void buildErrorMessageForLogs_includes_orig_forwarded_request_uri_when_available_in_request_attrs(
        String forwardedRequestUriAttrValue,
        String forwardedPathInfoAttrValue,
        String expectedResult
    ) {
        // given
        StringBuilder sb = new StringBuilder();
        RequestInfoForLogging request = mock(RequestInfoForLogging.class);

        doReturn(forwardedRequestUriAttrValue).when(request).getAttribute("javax.servlet.forward.request_uri");
        doReturn(forwardedPathInfoAttrValue).when(request).getAttribute("javax.servlet.forward.path_info");

        // when
        impl.buildErrorMessageForLogs(
            sb,
            request,
            Collections.emptyList(),
            400,
            new RuntimeException("intentional test exception"), Collections.emptyList()
        );

        // then
        String result = sb.toString();
        if (expectedResult == null) {
            Assertions.assertThat(result).doesNotContain("orig_forwarded_request_uri");
        }
        else {
            Assertions.assertThat(result)
                      .contains(String.format("orig_forwarded_request_uri=\"%s\"", expectedResult));
        }
    }

    @Test
    public void parseSpecificHeaderToStringShouldWorkForHappyPathWithOneHeaderVal() {
        when(reqMock.getHeaders("foo")).thenReturn(Arrays.asList("fooval"));

        String result = impl.parseSpecificHeaderToString(reqMock, "foo");
        assertThat(result, is("foo=fooval"));
    }

    @Test
    public void parseSpecificHeaderToStringShouldWorkForHappyPathWithMultipleHeaderVals() {
        when(reqMock.getHeaders("foo")).thenReturn(Arrays.asList("fooval1", "fooval2"));

        String result = impl.parseSpecificHeaderToString(reqMock, "foo");
        assertThat(result, is("foo=[fooval1,fooval2]"));
    }

    @Test
    public void parseSpecificHeaderToStringShouldReturnBlankStringIfHeadersIsNull() {
        when(reqMock.getHeaders("foo")).thenReturn(null);

        String result = impl.parseSpecificHeaderToString(reqMock, "foo");
        assertThat(result, is(""));
        verify(reqMock).getHeaders("foo");
    }

    @Test
    public void parseSpecificHeaderToStringShouldReturnBlankStringIfHeadersIsEmpty() {
        when(reqMock.getHeaders("foo")).thenReturn(Collections.<String>emptyList());

        String result = impl.parseSpecificHeaderToString(reqMock, "foo");
        assertThat(result, is(""));
        verify(reqMock).getHeaders("foo");
    }

    @Test
    public void parseSpecificHeaderToStringShouldReturnBlankStringIfUnexpectedExceptionOccurs() {
        when(reqMock.getHeaders("foo")).thenThrow(new RuntimeException());

        String result = impl.parseSpecificHeaderToString(reqMock, "foo");
        assertThat(result, is(""));
        verify(reqMock).getHeaders("foo");
    }
    
    @Test
    public void parseRequestHeadersToStringShouldWorkForHappyPath() {
        when(reqMock.getHeadersMap()).thenReturn(new TreeMap<>(MapBuilder.<String, List<String>>builder()
                                                           .put("header1", Arrays.asList("h1val"))
                                                           .put("header2", Arrays.asList("h2val1", "h2val2"))
                                                           .build()));
        when(reqMock.getHeaders("header1")).thenReturn(Arrays.asList("h1val"));
        when(reqMock.getHeaders("header2")).thenReturn(Arrays.asList("h2val1", "h2val2"));

        String result = impl.parseRequestHeadersToString(reqMock);
        assertThat(result, is("header1=h1val,header2=[h2val1,h2val2]"));
    }

    @Test
    public void parseRequestHeadersToStringShouldWorkForSpecialHeadersPath() {
        ApiExceptionHandlerUtils customImpl = new ApiExceptionHandlerUtils(true, new HashSet<>(Arrays.asList("Authorization", "X-Some-Alt-Authorization")), null);
        when(reqMock.getHeadersMap()).thenReturn(new TreeMap<>(MapBuilder.<String, List<String>>builder()
            .put("Authorization", Arrays.asList("secret secret"))
            .put("X-Some-Alt-Authorization", Arrays.asList("secret1 secret2"))
            .build()));
        when(reqMock.getHeaders("Authorization")).thenReturn(Arrays.asList("secret secret"));
        when(reqMock.getHeaders("X-Some-Alt-Authorization")).thenReturn(Arrays.asList("secret1 secret2"));

        String result = customImpl.parseRequestHeadersToString(reqMock);
        assertThat(result, is("Authorization=[MASKED],X-Some-Alt-Authorization=[MASKED]"));
    }

    @Test
    public void parseRequestHeadersToStringShouldReturnBlankStringIfHeadersMapIsNull() {
        when(reqMock.getHeadersMap()).thenReturn(null);

        String result = impl.parseRequestHeadersToString(reqMock);
        assertThat(result, is(""));
        verify(reqMock).getHeadersMap();
    }

    @Test
    public void parseRequestHeadersToStringShouldReturnBlankStringIfHeaderNamesIsEmpty() {
        when(reqMock.getHeadersMap()).thenReturn(Collections.<String, List<String>>emptyMap());

        String result = impl.parseRequestHeadersToString(reqMock);
        assertThat(result, is(""));
        verify(reqMock).getHeadersMap();
    }

    @Test
    public void parseRequestHeadersToStringShouldReturnBlankStringIfUnexpectedExceptionOccurs() {
        when(reqMock.getHeadersMap()).thenThrow(new RuntimeException("intentional test exception"));

        String result = impl.parseRequestHeadersToString(reqMock);
        assertThat(result, is(""));
        verify(reqMock).getHeadersMap();
    }

    @Test
    public void concatenateErrorCollectionShouldWorkWithOneItemInCollection() {
        String result = impl.concatenateErrorCollection(Arrays.<ApiError>asList(BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT));
        assertThat(result, is("MISSING_EXPECTED_CONTENT"));
    }

    @Test
    public void concatenateErrorCollectionShouldWorkWithMultipleItemsInCollection() {
        String result = impl.concatenateErrorCollection(Arrays.<ApiError>asList(BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT, BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR));
        assertThat(result, is("MISSING_EXPECTED_CONTENT,TYPE_CONVERSION_ERROR"));
    }

    @Test
    public void concatenateErrorCollectionShouldReturnBlankStringWhenPassedNull() {
        String result = impl.concatenateErrorCollection(null);
        assertThat(result, is(""));
    }

    @Test
    public void concatenateErrorCollectionShouldReturnBlankStringWhenPassedEmptyCollection() {
        String result = impl.concatenateErrorCollection(new ArrayList<ApiError>());
        assertThat(result, is(""));
    }

    private static class FakeHeader {
        public final String headerName;
        public final List<String> headerValues;

        private FakeHeader(String headerName, String ... headerValuesArray) {
            this.headerName = headerName;
            this.headerValues = new ArrayList<>();
            headerValues.addAll(Arrays.asList(headerValuesArray));
        }
    }
}
