package com.nike.backstopper.handler.adapter;

import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link com.nike.backstopper.handler.adapter.RequestInfoForLoggingServletApiAdapter}
 */
public class RequestInfoForLoggingServletApiAdapterTest {

    private RequestInfoForLoggingServletApiAdapter adapter;
    private HttpServletRequest requestMock;

    @Before
    public void beforeMethod() {
        requestMock = mock(HttpServletRequest.class);
        adapter = new RequestInfoForLoggingServletApiAdapter(requestMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfPassedNullRequest() {
        new RequestInfoForLoggingServletApiAdapter(null);
    }

    @Test
    public void getRequestUriDelegatesToServletRequest() {
        String expectedValue = UUID.randomUUID().toString();
        doReturn(expectedValue).when(requestMock).getRequestURI();
        assertThat(adapter.getRequestUri(), is(expectedValue));
    }

    @Test
    public void getRequestHttpMethodDelegatesToServletRequest() {
        String expectedValue = UUID.randomUUID().toString();
        doReturn(expectedValue).when(requestMock).getMethod();
        assertThat(adapter.getRequestHttpMethod(), is(expectedValue));
    }

    @Test
    public void getQueryStringDelegatesToServletRequest() {
        String expectedValue = UUID.randomUUID().toString();
        doReturn(expectedValue).when(requestMock).getQueryString();
        assertThat(adapter.getQueryString(), is(expectedValue));
    }

    @Test
    public void getHeaderMapDelegatesToServletRequestAndCachesResult() {
        Map<String, List<String>> expectedHeaderMap = new TreeMap<>(MapBuilder.<String, List<String>>builder()
                                                                              .put("header1", List.of("h1val1"))
                                                                              .put("header2", Arrays.asList("h2val1", "h2val2"))
                                                                              .build());
        doReturn(Collections.enumeration(expectedHeaderMap.keySet())).when(requestMock).getHeaderNames();
        for (Map.Entry<String, List<String>> entry : expectedHeaderMap.entrySet()) {
            doReturn(Collections.enumeration(entry.getValue())).when(requestMock).getHeaders(entry.getKey());
        }
        Map<String, List<String>> actualHeaderMap = adapter.getHeadersMap();
        assertThat(actualHeaderMap, is(expectedHeaderMap));
        assertThat(adapter.getHeadersMap(), sameInstance(actualHeaderMap));
    }

    @Test
    public void getHeaderMapReturnsEmptyMapIfServletRequestHeaderNamesReturnsNull() {
        doReturn(null).when(requestMock).getHeaderNames();
        Map<String, List<String>> actualHeaderMap = adapter.getHeadersMap();
        assertThat(actualHeaderMap, notNullValue());
        assertThat(actualHeaderMap.isEmpty(), is(true));
    }

    @Test
    public void getHeaderMapIgnoresHeadersWhereServletRequestGetHeadersMethodReturnsNull() {
        Map<String, List<String>> expectedHeaderMap = new TreeMap<>(MapBuilder.<String, List<String>>builder()
                                                                              .put("header1", List.of("h1val1"))
                                                                              .build());
        doReturn(Collections.enumeration(Arrays.asList("header1", "header2"))).when(requestMock).getHeaderNames();
        doReturn(Collections.enumeration(expectedHeaderMap.get("header1"))).when(requestMock).getHeaders("header1");
        doReturn(null).when(requestMock).getHeaders("header2");
        assertThat(adapter.getHeadersMap(), is(expectedHeaderMap));
    }

    @Test
    public void getHeaderDelegatesToServletRequest() {
        String headerName = "someheader";
        String expectedValue = UUID.randomUUID().toString();
        doReturn(expectedValue).when(requestMock).getHeader(headerName);
        assertThat(adapter.getHeader(headerName), is(expectedValue));
    }

    @Test
    public void getHeadersDelegatesToServletRequest() {
        Pair<String, List<String>> header1 = Pair.of("header1", List.of("h1val1"));
        Pair<String, List<String>> header2 = Pair.of("header2", Arrays.asList("h2val1", "h2val2"));
        Map<String, List<String>> expectedHeaderMap = new TreeMap<>(MapBuilder.<String, List<String>>builder()
                                                                              .put(header1.getKey(), header1.getValue())
                                                                              .put(header2.getKey(), header2.getValue())
                                                                              .build());
        doReturn(Collections.enumeration(expectedHeaderMap.keySet())).when(requestMock).getHeaderNames();
        for (Map.Entry<String, List<String>> entry : expectedHeaderMap.entrySet()) {
            doReturn(Collections.enumeration(entry.getValue())).when(requestMock).getHeaders(entry.getKey());
        }
        assertThat(adapter.getHeaders(header1.getKey()), is(header1.getValue()));
        assertThat(adapter.getHeaders(header2.getKey()), is(header2.getValue()));
    }

    @Test
    public void getAttributeDelegatesToServletRequest() {
        String attributeName = "someattribute";
        UUID expectedValue = UUID.randomUUID();
        doReturn(expectedValue).when(requestMock).getAttribute(attributeName);
        assertThat(adapter.getAttribute(attributeName), Is.is(expectedValue));
    }

    @Test
    public void testGetBodyReturnsCorrectBody() throws Exception {
        String expected = "this is the expected string";
        ByteArrayInputStream bais = new ByteArrayInputStream(expected.getBytes());
        try (ServletInputStream is = new DelegatingServletInputStream(bais)) {
            doReturn(is).when(requestMock).getInputStream();
            doReturn("UTF-8").when(requestMock).getCharacterEncoding();
            String actual = adapter.getBody();
            assertThat(actual, is(expected));
        }
    }

    @Test
    public void testGetBodyWithNullCharacterEncodingReturnsCorrectBody() throws Exception {
        String expected = "this is the expected string";
        ByteArrayInputStream bais = new ByteArrayInputStream(expected.getBytes());
        try (ServletInputStream is = new DelegatingServletInputStream(bais)) {
            doReturn(is).when(requestMock).getInputStream();
            String actual = adapter.getBody();
            assertThat(actual, is(expected));
        }
    }

    @Test(expected= RequestInfoForLogging.GetBodyException.class)
    public void testGetBodyThrowsExceptionOnError() throws Exception {
        BufferedReader mockReader = mock(BufferedReader.class);
        doReturn(-1).when(mockReader).read(any(java.nio.CharBuffer.class));
        doThrow(new IOException("this happened")).when(requestMock).getReader();
        adapter.getBody();
    }

    @Test
    public void getBody_does_not_explode_if_input_stream_explodes_during_close()
        throws IOException, RequestInfoForLogging.GetBodyException {
        // given
        String expected = "this is the expected string";
        ByteArrayInputStream bais = new ByteArrayInputStream(expected.getBytes());
        ServletInputStream is = spy(new DelegatingServletInputStream(bais));
        doReturn(is).when(requestMock).getInputStream();
        doReturn("UTF-8").when(requestMock).getCharacterEncoding();
        doThrow(new RuntimeException("kaboom")).when(is).close();

        // when
        String actual = adapter.getBody();

        // then
        assertThat(actual, is(expected));
        verify(is, atLeastOnce()).close();
    }

    private static class DelegatingServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;

        /**
         * Create a DelegatingServletInputStream for the given source stream.
         * @param sourceStream the source stream (never <code>null</code>)
         */
        public DelegatingServletInputStream(InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }

        /**
         * Return the underlying source stream (never <code>null</code>).
         */
        public final InputStream getSourceStream() {
            return this.sourceStream;
        }


        public int read() throws IOException {
            return this.sourceStream.read();
        }

        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        @Override
        public boolean isFinished() {
            try {
                return this.sourceStream.available() <= 0;
            }
            catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("An error occurred asking for available bytes from the underlying stream.", e);
                return true;
            }
        }

        @Override
        public boolean isReady() {
            try {
                return this.sourceStream.available() > 0;
            }
            catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("An error occurred asking for available bytes from the underlying stream.", e);
                return false;
            }
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Do nothing.
        }
    }
}