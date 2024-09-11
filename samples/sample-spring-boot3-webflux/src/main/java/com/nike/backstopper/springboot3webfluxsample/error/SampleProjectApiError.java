package com.nike.backstopper.springboot3webfluxsample.error;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.springboot3webfluxsample.model.RgbColor;
import com.nike.internal.util.MapBuilder;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Project-specific error definitions for this sample app. Note that the error codes for errors specified here must
 * conform to the range specified in {@link SampleProjectApiErrorsImpl#getProjectSpecificErrorCodeRange()} or an
 * exception will be thrown on app startup, and unit tests should fail. The one exception to this rule is a "core
 * error wrapper" - an instance that shares the same error code, message, and HTTP status code as a
 * {@code SampleProjectApiErrorsImpl.getCoreApiErrors()} instance (in this case that means a wrapper around
 * {@link SampleCoreApiError}).
 *
 * @author Nic Munroe
 */
public enum SampleProjectApiError implements ApiError {
    FIELD_CANNOT_BE_NULL_OR_BLANK(99100, "Field cannot be null or empty", HttpStatus.BAD_REQUEST.value()),
    // FOO_STRING_CANNOT_BE_BLANK shows how you can build off a base/generic error and add metadata.
    FOO_STRING_CANNOT_BE_BLANK(FIELD_CANNOT_BE_NULL_OR_BLANK, MapBuilder.builder("field", (Object)"foo").build()),
    INVALID_RANGE_VALUE(99110, "The range_0_to_42 field must be between 0 and 42 (inclusive)",
                        HttpStatus.BAD_REQUEST.value()),
    // RGB_COLOR_CANNOT_BE_NULL could build off FIELD_CANNOT_BE_NULL_OR_BLANK like FOO_STRING_CANNOT_BE_BLANK does,
    //      however this shows how you can make individual field errors with unique code and custom message.
    RGB_COLOR_CANNOT_BE_NULL(99120, "The rgb_color field must be defined", HttpStatus.BAD_REQUEST.value()),
    NOT_RGB_COLOR_ENUM(99130, "The rgb_color field value must be one of: " + Arrays.toString(RgbColor.values()),
                       HttpStatus.BAD_REQUEST.value()),
    MANUALLY_THROWN_ERROR(99140, "You asked for an error to be thrown", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    // This is a wrapper around a core error. It will have the same error code, message, and HTTP status code,
    //      but will show up in the logs with contributing_errors="SOME_MEANINGFUL_ERROR_NAME", allowing you to
    //      distinguish the context of the error vs. the core GENERIC_SERVICE_ERROR at a glance.
    SOME_MEANINGFUL_ERROR_NAME(SampleCoreApiError.GENERIC_SERVICE_ERROR),
    ERROR_THROWN_IN_WEB_FILTER(
        99150, "An error was thrown in a WebFilter", HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    ERROR_RETURNED_IN_WEB_FILTER_MONO(
        99151, "An error was returned in a WebFilter Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION(
        99155, "An error was thrown in a HandlerFilterFunction", HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO(
        99156, "An error was returned in a HandlerFilterFunction Mono", HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    WEBFLUX_MONO_ERROR(
        99160, "You hit the WebFlux Mono error endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    WEBFLUX_FLUX_ERROR(
        99170, "You hit the WebFlux Flux error endpoint", HttpStatus.INTERNAL_SERVER_ERROR.value()
    );

    private final ApiError delegate;

    SampleProjectApiError(ApiError delegate) {
        this.delegate = delegate;
    }

    SampleProjectApiError(ApiError delegate, Map<String, Object> metadata) {
        this(new ApiErrorWithMetadata(delegate, metadata));
    }

    SampleProjectApiError(int errorCode, String message, int httpStatusCode) {
        this(new ApiErrorBase(
            "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), errorCode, message, httpStatusCode
        ));
    }

    @SuppressWarnings("unused")
    SampleProjectApiError(int errorCode, String message, int httpStatusCode, Map<String, Object> metadata) {
        this(new ApiErrorBase(
            "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), errorCode, message, httpStatusCode, metadata
        ));
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getErrorCode() {
        return delegate.getErrorCode();
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public int getHttpStatusCode() {
        return delegate.getHttpStatusCode();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return delegate.getMetadata();
    }

}