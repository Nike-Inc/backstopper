package com.nike.backstopper.util;

import com.nike.backstopper.apierror.ApiError;

import java.util.Objects;

/**
 * Utility class to enable sharing code between {@link ApiError} implementations. Most Backstopper end-users
 * won't need to use this class unless you're creating custom {@link ApiError} implementations (which is not
 * common).
 */
public class ApiErrorUtil {

    /**
     * Method for generating a hashcode for the given {@link ApiError} . This can be used in implementations of
     * {@link ApiError}.
     */
    public static int generateApiErrorHashCode(ApiError apiError) {
        return Objects.hash(apiError.getName(), apiError.getErrorCode(), apiError.getMessage(), apiError.getHttpStatusCode(), apiError.getMetadata());
    }

    /**
     * Method for checking equality of two {@link ApiError}. This can be used in implementations of {@link ApiError}
     */
    public static boolean isApiErrorEqual(ApiError apiError, Object o) {
        if (apiError == o) return true;
        if (apiError == null) return false;
        if (o == null || !(o instanceof ApiError that)) return false;
        return apiError.getHttpStatusCode() == that.getHttpStatusCode() &&
                Objects.equals(apiError.getName(), that.getName()) &&
                Objects.equals(apiError.getErrorCode(), that.getErrorCode()) &&
                Objects.equals(apiError.getMessage(), that.getMessage()) &&
                Objects.equals(apiError.getMetadata(), that.getMetadata());
    }
}
