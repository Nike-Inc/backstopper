package com.nike.backstopper.handler.adapter;

import com.nike.backstopper.handler.RequestInfoForLogging.GetBodyException;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link RequestInfoForLoggingWebFluxAdapter}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class RequestInfoForLoggingWebFluxAdapterTest {

    private ServerRequest requestMock;
    private URI requestUri;
    private String httpMethodName;
    private ServerRequest.Headers serverRequestHeadersMock;
    private HttpHeaders headers;
    private RequestInfoForLoggingWebFluxAdapter adapter;

    private static final String REQUEST_PATH = "/foo/bar";
    private static final String QUERY_STRING = "someQuery=someValue";

    @Before
    public void beforeMethod() {
        requestMock = mock(ServerRequest.class);
        requestUri = URI.create(String.format("http://localhost:8080%s?%s", REQUEST_PATH, QUERY_STRING));
        httpMethodName = UUID.randomUUID().toString();
        headers = new HttpHeaders();

        serverRequestHeadersMock = mock(ServerRequest.Headers.class);

        doReturn(requestUri).when(requestMock).uri();
        doReturn(httpMethodName).when(requestMock).methodName();
        doReturn(serverRequestHeadersMock).when(requestMock).headers();

        doReturn(headers).when(serverRequestHeadersMock).asHttpHeaders();

        adapter = new RequestInfoForLoggingWebFluxAdapter(requestMock);
    }

    @Test
    public void constructor_sets_fields_as_expected() {
        // given
        requestMock = mock(ServerRequest.class);
        doReturn(requestUri).when(requestMock).uri();
        
        // when
        RequestInfoForLoggingWebFluxAdapter adapter = new RequestInfoForLoggingWebFluxAdapter(requestMock);

        // then
        assertThat(adapter.request).isSameAs(requestMock);
        assertThat(adapter.requestUri).isSameAs(requestUri);
        verify(requestMock).uri();
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_null_ServerRequest() {
        // when
        Throwable ex = catchThrowable(() -> new RequestInfoForLoggingWebFluxAdapter(null));

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("request cannot be null");
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_ServerRequest_with_null_URI() {
        // given
        doReturn(null).when(requestMock).uri();

        // when
        Throwable ex = catchThrowable(() -> new RequestInfoForLoggingWebFluxAdapter(requestMock));

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("request.uri() cannot be null");
    }

    @Test
    public void getRequestUri_returns_the_request_path() {
        // when
        String result = adapter.getRequestUri();

        // then
        assertThat(result).isEqualTo(REQUEST_PATH);
    }

    @Test
    public void getRequestHttpMethod_returns_the_request_methodName() {
        // when
        String result = adapter.getRequestHttpMethod();

        // then
        assertThat(result).isEqualTo(httpMethodName);
    }

    @Test
    public void getQueryString_returns_the_request_query_string() {
        // when
        String result = adapter.getQueryString();

        // then
        assertThat(result).isEqualTo(QUERY_STRING);
    }

    @Test
    public void getHeadersMap_returns_the_request_headers_asHttpHeaders() {
        // given
        headers = new HttpHeaders();
        serverRequestHeadersMock = mock(ServerRequest.Headers.class);

        doReturn(serverRequestHeadersMock).when(requestMock).headers();
        doReturn(headers).when(serverRequestHeadersMock).asHttpHeaders();

        // when
        Map<String, List<String>> result = adapter.getHeadersMap();

        // then
        assertThat(result).isSameAs(headers);
        verify(requestMock).headers();
        verify(serverRequestHeadersMock).asHttpHeaders();
    }

    private enum GetHeaderScenario {
        NULL_RESULT(null, null),
        EMPTY_RESULT(Collections.emptyList(), null),
        SINGLE_RESULT(Collections.singletonList("foo"), "foo"),
        MULTIPLE_RESULTS(Arrays.asList("foo", "bar"), "foo");

        public final List<String> headerValues;
        public final String expectedResult;

        GetHeaderScenario(List<String> headerValues, String expectedResult) {
            this.headerValues = headerValues;
            this.expectedResult = expectedResult;
        }
    }

    @DataProvider
    public static List<List<GetHeaderScenario>> getHeaderScenarioDataProvider() {
        return Stream.of(GetHeaderScenario.values()).map(Collections::singletonList).collect(Collectors.toList());
    }

    @UseDataProvider("getHeaderScenarioDataProvider")
    @Test
    public void getHeader_returns_the_request_headers_asHttpHeaders(GetHeaderScenario scenario) {
        // given
        String headerName = UUID.randomUUID().toString();
        doReturn(scenario.headerValues).when(serverRequestHeadersMock).header(headerName);

        // when
        String result = adapter.getHeader(headerName);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
        verify(serverRequestHeadersMock).header(headerName);
    }

    @Test
    public void getHeaders_returns_the_list_of_header_values() {
        // given
        String headerName = UUID.randomUUID().toString();
        List<String> expectedResult = Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        doReturn(expectedResult).when(serverRequestHeadersMock).header(headerName);

        // when
        List<String> result = adapter.getHeaders(headerName);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(serverRequestHeadersMock).header(headerName);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void getAttribute_works_as_expected(boolean attributeIsNull) {
        // given
        String attrKey = UUID.randomUUID().toString();
        Object expectedResult = (attributeIsNull) ? null : new Object();

        doReturn(Optional.ofNullable(expectedResult)).when(requestMock).attribute(attrKey);

        // when
        Object result = adapter.getAttribute(attrKey);

        // then
        assertThat(result).isSameAs(expectedResult);
    }

    @Test
    public void getBody_throws_GetBodyException_with_UnsupportedOperationException_cause() {
        // when
        Throwable ex = catchThrowable(() -> adapter.getBody());

        // then
        assertThat(ex)
            .isInstanceOf(GetBodyException.class)
            .hasMessage("Cannot extract the body from a WebFlux ServerRequest.")
            .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}