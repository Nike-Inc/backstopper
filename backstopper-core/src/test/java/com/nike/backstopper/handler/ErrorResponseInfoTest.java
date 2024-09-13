package com.nike.backstopper.handler;

import com.nike.internal.util.MapBuilder;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Unit test for {@link ErrorResponseInfo}
 */
public class ErrorResponseInfoTest {

    @Test
    public void constructorSetsValues() {
        Object frameworkObj = new Object();
        Map<String, List<String>> headersMap = MapBuilder
            .<String, List<String>>builder()
            .put("header1", List.of("val1"))
            .put("header2", List.of("h2val1, h2val2"))
            .build();
        ErrorResponseInfo<Object> responseInfo = new ErrorResponseInfo<>(42, frameworkObj, headersMap);
        assertThat(responseInfo.frameworkRepresentationObj, is(frameworkObj));
        assertThat(responseInfo.headersToAddToResponse, is(headersMap));
        assertThat(responseInfo.httpStatusCode, is(42));
    }

    @Test
    public void constructorHandlesNullValuesGracefully() {
        ErrorResponseInfo<Object> responseInfo = new ErrorResponseInfo<>(42, null, null);
        assertThat(responseInfo.frameworkRepresentationObj, nullValue());
        assertThat(responseInfo.headersToAddToResponse, notNullValue());
        assertThat(responseInfo.headersToAddToResponse.isEmpty(), is(true));
    }

}