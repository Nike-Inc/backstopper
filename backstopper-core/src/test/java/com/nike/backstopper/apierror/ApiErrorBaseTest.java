package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;

import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the functionality of {@link ApiErrorBase}
 */
public class ApiErrorBaseTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldThrowIllegalArgumentExceptionIfPassedNullName() {
        new ApiErrorBase(null, 42, "some error", 400);
    }

    @Test
    public void mirrorConstructorShouldCopyAllFields() {
        String name = "someName";
        int errorCode = 42;
        String message = "some error";
        int httpStatusCode = 400;
        Map<String, Object> metadata = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();
        ApiErrorBase aeb = new ApiErrorBase(name, errorCode, message, httpStatusCode, metadata);
        ApiErrorBase mirror = new ApiErrorBase(aeb);

        assertThat(mirror.getName(), is(name));
        assertThat(mirror.getErrorCode(), is(String.valueOf(errorCode)));
        assertThat(mirror.getMessage(), is(message));
        assertThat(mirror.getHttpStatusCode(), is(httpStatusCode));
        assertThat(mirror.getMetadata(), is(metadata));
    }

    @Test
    public void getMetadataNeverReturnsNull() {
        ApiErrorBase aeb = new ApiErrorBase("someName", 42, "some error", 400, null);
        assertThat(aeb.getMetadata(), notNullValue());
    }
}