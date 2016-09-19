package com.nike.backstopper.apierror.sample;

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
 * A sample/example of some core errors that many APIs are likely to need. Any given {@link ProjectApiErrors} could
 * return these as its {@link ProjectApiErrors#getCoreApiErrors()} if the error codes and messages associated with these
 * are fine for your project (see {@link SampleProjectApiErrorsBase} for a base implementation that does exactly that).
 *
 * <p>In practice most organizations should copy/paste this class and customize the error codes and messages for their
 * organization. If the organization-specific version is published in a reusable library then it can be shared around to
 * all projects that should use the same values for their core errors.
 *
 * @author Nic Munroe
 */
public enum SampleCoreApiError implements ApiError {
    GENERIC_SERVICE_ERROR(10, "An error occurred while fulfilling the request", HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR),
    // OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR (and the other wrappers around GENERIC_SERVICE_ERROR)
    //      intentionally contains values identical to GENERIC_SERVICE_ERROR (it's indistinguishable from the client
    //      point of view but lets us know what really went wrong in the logs)
    OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR(GENERIC_SERVICE_ERROR),
    SERVERSIDE_VALIDATION_ERROR(GENERIC_SERVICE_ERROR),
    UNHANDLED_FRAMEWORK_ERROR(GENERIC_SERVICE_ERROR),
    TEMPORARY_SERVICE_PROBLEM(20, "Service is temporarily unavailable, try again later",
                              HTTP_STATUS_CODE_SERVICE_UNAVAILABLE),
    OUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERROR(TEMPORARY_SERVICE_PROBLEM),
    INVALID_REQUEST(30, "Invalid request", HTTP_STATUS_CODE_BAD_REQUEST),
    MISSING_EXPECTED_CONTENT(40, "Missing expected content", HTTP_STATUS_CODE_BAD_REQUEST),
    TYPE_CONVERSION_ERROR(50, "Type conversion error", HTTP_STATUS_CODE_BAD_REQUEST),
    MALFORMED_REQUEST(60, "Malformed request", HTTP_STATUS_CODE_BAD_REQUEST),
    INVALID_ACCEPTS_HEADER(70, "Invalid or missing header: Accepts", HTTP_STATUS_CODE_BAD_REQUEST),
    INVALID_CONTENT_TYPE_HEADER(80, "Invalid or missing header: Content-Type", HTTP_STATUS_CODE_BAD_REQUEST),
    UNAUTHORIZED(90, "Unauthorized access", HTTP_STATUS_CODE_UNAUTHORIZED),
    FORBIDDEN(100, "Forbidden access", HTTP_STATUS_CODE_FORBIDDEN),
    NOT_FOUND(110, "The requested resource was not found", HTTP_STATUS_CODE_NOT_FOUND),
    METHOD_NOT_ALLOWED(120, "Http Request method not allowed for this resource", HTTP_STATUS_CODE_METHOD_NOT_ALLOWED),
    NO_ACCEPTABLE_REPRESENTATION(130, "No acceptable representation for this resource",
                                 HTTP_STATUS_CODE_NOT_ACCEPTABLE),
    UNSUPPORTED_MEDIA_TYPE(140, "Unsupported media type", HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS(150, "Too many requests or simultaneous requests not allowed for this endpoint",
                      HTTP_STATUS_CODE_TOO_MANY_REQUESTS);

    private final ApiError delegate;

    SampleCoreApiError(ApiError delegate) {
        this.delegate = delegate;
    }

    SampleCoreApiError(int errorCode, String message, int httpStatusCode) {
        this(new ApiErrorBase(
            "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), errorCode, message, httpStatusCode
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
