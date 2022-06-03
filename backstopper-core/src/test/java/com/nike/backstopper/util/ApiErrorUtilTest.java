package com.nike.backstopper.util;

import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.internal.util.MapBuilder;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.nike.backstopper.util.ApiErrorUtil.isApiErrorEqual;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ApiErrorUtilTest {

    @Test
    public void constructor_code_coverage() {
        // given
        ApiErrorUtil util = new ApiErrorUtil();

        // then
        assertThat(util).isNotNull();
    }

    @Test
    public void generate_api_error_hashcode_generates_expected_hashcodes() {
        // given
        Map<String, Object> metadata = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();
        ApiErrorBase apiErrorBase = new ApiErrorBase("name", 42, "errorMessag", 400, metadata);
        ApiErrorBase apiErrorBaseCopy = new ApiErrorBase("name", 42, "errorMessag", 400, metadata);

        // then
        assertThat(ApiErrorUtil.generateApiErrorHashCode(apiErrorBase))
                .isEqualTo(Objects.hash(
                        apiErrorBase.getName(),
                        apiErrorBase.getErrorCode(),
                        apiErrorBase.getMessage(),
                        apiErrorBase.getHttpStatusCode(),
                        apiErrorBase.getMetadata())
                );

        assertThat(apiErrorBase).isEqualTo(apiErrorBaseCopy);
        assertThat(ApiErrorUtil.generateApiErrorHashCode(apiErrorBase))
                .isEqualTo(ApiErrorUtil.generateApiErrorHashCode(apiErrorBaseCopy));
    }

    @Test
    public void equals_same_object_is_true() {
        // given
        ApiErrorBase apiError = new ApiErrorBase("name", 42, "errorMessage", 400);

        // then
        assertThat(isApiErrorEqual(apiError, apiError)).isTrue();
    }

    @Test
    @DataProvider(value = {
            "false | true  | false",
            "false | false | false",
            "true  | false | false",
            "true  | true  | true"
    }, splitBy = "\\|")
    public void equals_null_other_class(boolean firstClassNull, boolean secondClassNull, boolean expectedValue) {
        // given
        ApiErrorBase apiError = new ApiErrorBase("name", 42, "errorMessage", 400);

        String otherClass = secondClassNull ? null : "";

        // then
        assertThat(isApiErrorEqual(firstClassNull ? null : apiError, otherClass)).isEqualTo(expectedValue);
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
                changeName ? "name2" : name,
                changeErrorCode ? 43 : errorCode,
                changeErrorMessage ? "message2" : message,
                changeHttpStatusCode ? 500 : httpStatusCode,
                changeMetadata ? metadata2 : metadata);

        // then
        assertThat(isApiErrorEqual(aeb, aeb2)).isEqualTo(isEqual);
    }
}
