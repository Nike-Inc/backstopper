package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link ApiErrorBase}
 */
@RunWith(DataProviderRunner.class)
public class ApiErrorBaseTest {

    @Test
    public void constructor_should_throw_IllegalArgumentException_null_name() {
        // when
        Throwable ex = catchThrowable(() -> new ApiErrorBase(null, 42, "some error", 400));

        // then
        assertThat(ex)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ApiError name cannot be null");
    }

    @Test
    public void constructor_should_throw_IllegalArgumentException_null_error_code() {
        // when
        Throwable ex = catchThrowable(() -> new ApiErrorBase(UUID.randomUUID().toString(), null, "some error", 400));

        // then
        assertThat(ex)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ApiError errorCode cannot be null");
    }

    @Test
    public void mirrorConstructor_should_copy_all_fields() {
        // given
        String name = "someName";
        int errorCode = 42;
        String message = "some error";
        int httpStatusCode = 400;
        Map<String, Object> metadata = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();
        ApiErrorBase aeb = new ApiErrorBase(name, errorCode, message, httpStatusCode, metadata);
        ApiErrorBase mirror = new ApiErrorBase(aeb);

        // then
        assertThat(mirror.getName()).isEqualTo(name);
        assertThat(mirror.getErrorCode()).isEqualTo(String.valueOf(errorCode));
        assertThat(mirror.getMessage()).isEqualTo(message);
        assertThat(mirror.getHttpStatusCode()).isEqualTo(httpStatusCode);
        assertThat(mirror.getMetadata()).isEqualTo(metadata);
        assertThat(aeb).isEqualTo(mirror);
    }

    @Test
    public void noMetadataStringErrorCodeConstructor_creates_as_expected() {
        // given
        String name = "someName";
        String errorCode = "42";
        String message = "some error";
        int httpStatusCode = 400;

        ApiErrorBase aeb = new ApiErrorBase(name, errorCode, message, httpStatusCode);

        // then
        assertThat(aeb.getName()).isEqualTo(name);
        assertThat(aeb.getErrorCode()).isEqualTo(String.valueOf(errorCode));
        assertThat(aeb.getMessage()).isEqualTo(message);
        assertThat(aeb.getHttpStatusCode()).isEqualTo(httpStatusCode);
    }

    @Test
    public void getMetadata_never_returns_null() {
        // given
        ApiErrorBase aeb = new ApiErrorBase("someName", 42, "some error", 400, null);

        // then
        assertThat(aeb.getMetadata()).isNotNull();
    }

    @Test
    public void hashcode_is_as_expected() {
        // given
        ApiErrorBase error = new ApiErrorBase("name", 42, "errorMessage", 400);
        ApiErrorBase error2 = new ApiErrorBase("name", 42, "errorMessage", 400);

        // then
        assertThat(error).isEqualTo(error2);
        assertThat(error.hashCode()).isEqualTo(error.hashCode());
        assertThat(error.hashCode()).isEqualTo(error2.hashCode());
    }

    @Test
    public void equals_same_object_is_true() {
        // given
        ApiErrorBase error = new ApiErrorBase("name", 42, "errorMessage", 400);

        // then
        assertThat(error.equals(error)).isTrue();
    }

    @Test
    @DataProvider(value = {
            "true",
            "false"
    })
    public void equals_null_or_other_class_is_false(boolean useNull) {
        // given
        ApiErrorBase error = new ApiErrorBase("name", 42, "errorMessage", 400);

        String otherClass = useNull? null : "";

        // then
        assertThat(error.equals(otherClass)).isFalse();
    }

    @Test
    @DataProvider(value = {
            "true  | false | false | false | false | false ",
            "false | false | false | false | false | true  ",
            "false | true  | false | false | false | false ",
            "false | false | true  | false | false | false ",
            "false | false | false | true  | false | false ",
            "false | false | false | false | true  | false ",
    }, splitBy = "\\|")
    public void equals_returns_expected_result(boolean changeName, boolean changeErrorCode, boolean changeErrorMessage, boolean changeHttpStatusCode, boolean changeMetadata, boolean isEqual) {
        // given
        String name = "someName";
        int errorCode = 42;
        String message = "some error";
        int httpStatusCode = 400;
        Map<String, Object> metadata = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();
        Map<String, Object> metadata2 = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();

        ApiErrorBase aeb = new ApiErrorBase(name, errorCode, message, httpStatusCode, metadata);

        ApiErrorBase aeb2 = new ApiErrorBase(
                changeName? "name2" : name ,
                changeErrorCode? 43 : errorCode,
                changeErrorMessage? "message2" : message,
                changeHttpStatusCode? 500 : httpStatusCode,
                changeMetadata? metadata2 : metadata);

        // then
        assertThat(aeb.equals(aeb2)).isEqualTo(isEqual);
    }

}