package com.nike.backstopper.apierror.testutil;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import java.util.Map;
import java.util.UUID;

import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_BAD_REQUEST;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_FORBIDDEN;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_METHOD_NOT_ALLOWED;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_NOT_ACCEPTABLE;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_NOT_FOUND;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_SERVICE_UNAVAILABLE;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_TOO_MANY_REQUESTS;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_UNAUTHORIZED;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE;

/**
 * A barebones set of core errors that can be used for testing. See {@link ProjectApiErrorsForTesting} for a {@link
 * ProjectApiErrors} impl that uses this and is also intended for testing.
 *
 * @author Nic Munroe
 */
public enum BarebonesCoreApiErrorForTesting implements ApiError {
    GENERIC_SERVICE_ERROR(10, "An error occurred while fulfilling the request", HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR),
    OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR(GENERIC_SERVICE_ERROR),
    SERVERSIDE_VALIDATION_ERROR(GENERIC_SERVICE_ERROR),
    TEMPORARY_SERVICE_PROBLEM(20, "Service is temporarily unavailable, try again later",
                              HTTP_STATUS_CODE_SERVICE_UNAVAILABLE),
    OUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERROR(TEMPORARY_SERVICE_PROBLEM),
    GENERIC_BAD_REQUEST(30, "Invalid request", HTTP_STATUS_CODE_BAD_REQUEST),
    MISSING_EXPECTED_CONTENT(40, "Missing expected content", HTTP_STATUS_CODE_BAD_REQUEST),
    TYPE_CONVERSION_ERROR(50, "Type conversion error", HTTP_STATUS_CODE_BAD_REQUEST),
    MALFORMED_REQUEST(60, "Malformed request", HTTP_STATUS_CODE_BAD_REQUEST),
    UNAUTHORIZED(70, "Unauthorized access", HTTP_STATUS_CODE_UNAUTHORIZED),
    FORBIDDEN(80, "Forbidden access", HTTP_STATUS_CODE_FORBIDDEN),
    NOT_FOUND(90, "The requested resource was not found", HTTP_STATUS_CODE_NOT_FOUND),
    METHOD_NOT_ALLOWED(100, "Http Request method not allowed for this resource", HTTP_STATUS_CODE_METHOD_NOT_ALLOWED),
    NO_ACCEPTABLE_REPRESENTATION(110, "No acceptable representation for this resource",
                                 HTTP_STATUS_CODE_NOT_ACCEPTABLE),
    UNSUPPORTED_MEDIA_TYPE(120, "Unsupported media type", HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS(130, "Too many requests or simultaneous requests not allowed for this endpoint",
                      HTTP_STATUS_CODE_TOO_MANY_REQUESTS);

    private final ApiError delegate;

    BarebonesCoreApiErrorForTesting(ApiError delegate) {
        this.delegate = delegate;
    }

    BarebonesCoreApiErrorForTesting(int errorCode, String message, int httpStatusCode) {
        this(new ApiErrorBase(
            "delegated-to-enum-wrapper-" + UUID.randomUUID(), errorCode, message, httpStatusCode
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