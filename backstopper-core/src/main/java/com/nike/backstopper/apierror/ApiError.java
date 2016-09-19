package com.nike.backstopper.apierror;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import java.util.Map;

/**
 * Represents the errors that are returned to users when something goes wrong. These are returned by your project's
 * {@link ProjectApiErrors} and you should have instances that cover every 4xx or 5xx error that your project might need
 * to return to the user. The easiest way to accomplish this is with an enum - see
 * {@link com.nike.backstopper.apierror.sample.SampleCoreApiError} for an example.
 *
 * <p><b>IMPORTANT NOTE: The {@link #getName()} should be considered an instance's unique ID. NO TWO {@code ApiErrors}
 * SHOULD SHARE THE SAME NAME!</b>
 *
 * @author Nic Munroe
 */
public interface ApiError {

    /**
     * @return The canonical name for this instance. {@link ProjectApiErrors#convertToApiError(String)} uses this method
     *          to match ApiError instances when trying to convert a String to an ApiError. e.g. If you had an enum
     *          implement this interface it would return {@link Enum#name()}. <p>IMPORTANT NOTE: The name returned by
     *          this method should be considered this instance's unique ID. NO TWO {@code ApiErrors} SHOULD SHARE THE
     *          SAME NAME! This should never return null.
     */
    String getName();

    /**
     * @return The business/project error code this instance represents (not to be confused with the HTTP status code
     *          which can be retrieved via {@link #getHttpStatusCode()}). This should never change for a given error so
     *          clients can rely on it and write code against it. Although the return type is a string and you can
     *          technically return anything you want, it's recommended that you return a string that can be parsed to an
     *          integer via {@link Integer#parseInt(String)}. This should never be null.
     */
    String getErrorCode();

    /**
     * @return The client-facing message associated with this instance. This is provided as a human readable nicety and
     *          clients should consider it subject to change at any time. This may be null (although it's not
     *          recommended).
     */
    String getMessage();

    /**
     * @return Arbitrary metadata about the error that can be used to add extra info to help clients understand more
     *          about what went wrong. This should never be null - an empty map should be returned if there's no
     *          metadata associated with an error. For example, if you want to return a
     *          {@link ProjectApiErrors#getMissingExpectedContentApiError()} but want to indicate which field is missing
     *          you could add a "missing_field" -> "foo" mapping to the metadata. Use {@link ApiErrorWithMetadata} to
     *          wrap any pre-existing {@link ApiError} in order to add metadata to it.
     */
    Map<String, Object> getMetadata();

    /**
     * @return The HTTP status code associated with this instance (not to be confused with the business/project error
     *          code which can be retrieved via {@link #getErrorCode()}).
     */
    int getHttpStatusCode();

}
