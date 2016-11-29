package com.nike.backstopper.handler.listener;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.internal.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A data structure representing whether or not a {@link ApiExceptionHandlerListener} wants to handle a specific
 * exception, and if so what the {@link #errors}, {@link #extraDetailsForLogging}, and {@link #extraResponseHeaders}
 * should be.
 *
 * @author Nic Munroe
 */
public class ApiExceptionHandlerListenerResult {

    /**
     * True represents an exception that should be handled, false represents an exception that should be ignored.
     */
    public final boolean shouldHandleResponse;
    /**
     * The sorted set of {@link ApiError}s that should be represented as part of the exception handling
     * (only if the exception is handled - if it is ignored then this set will not be used). This will never be null
     * (an empty set will be used if there are no errors).
     */
    public final SortedApiErrorSet errors;
    /**
     * The list of extra details that should be logged as part of the exception handling (only if the exception is
     * handled - if it is ignored then this list will not be used). This will never be null, and it will always be a
     * mutable list so that more logging info can be added to it.
     */
    public final List<Pair<String, String>> extraDetailsForLogging;
    /**
     * The list of extra response headers that should be included with {@link ErrorResponseInfo#headersToAddToResponse}
     * after exception handling. The framework should include these as response headers in the response to the caller.
     * This will never be null, and it will always be a mutable list so that more headers can be added to it.
     */
    public final List<Pair<String, List<String>>> extraResponseHeaders;

    /**
     * Intentionally protected - outside callers should use one of the {@link #handleResponse(SortedApiErrorSet)},
     * {@link #handleResponse(SortedApiErrorSet, java.util.List)}, {@link
     * #handleResponse(SortedApiErrorSet, List, List)}, or {@link #ignoreResponse()} static factory methods for
     * generating a new instance.
     *
     * @param shouldHandleResponse
     *     Whether or not this instance contains the information for handling the exception.
     * @param errors
     *     The {@link ApiError}s that should represent the exception. This can be null or empty, but only if {@code
     *     shouldHandleResponse} is false.
     * @param extraDetailsForLogging
     *     Any extra key/value pairs that should be logged when Backstopper logs the exception/error, or null if you
     *     have nothing extra to log.
     * @param extraResponseHeaders
     *     Any extra response headers that the framework should include in the response to the caller, or null if you
     *     have no extra response headers to send.
     */
    protected ApiExceptionHandlerListenerResult(boolean shouldHandleResponse, SortedApiErrorSet errors,
                                                List<Pair<String, String>> extraDetailsForLogging,
                                                List<Pair<String, List<String>>> extraResponseHeaders) {
        this.shouldHandleResponse = shouldHandleResponse;
        if (errors == null)
            errors = new SortedApiErrorSet();
        this.errors = new SortedApiErrorSet(errors);
        if (extraDetailsForLogging == null)
            extraDetailsForLogging = Collections.emptyList();
        this.extraDetailsForLogging = new ArrayList<>(extraDetailsForLogging);
        if (extraResponseHeaders == null)
            extraResponseHeaders = Collections.emptyList();
        this.extraResponseHeaders = new ArrayList<>(extraResponseHeaders);
    }

    /**
     * @param errors
     *     The {@link ApiError}s that should represent the exception. This should not be null or empty.
     *
     * @return An instance with {@link #shouldHandleResponse} set to true and {@link #errors} set to a copy of the given
     * set of errors.
     */
    public static ApiExceptionHandlerListenerResult handleResponse(SortedApiErrorSet errors) {
        return new ApiExceptionHandlerListenerResult(true, errors, null, null);
    }

    /**
     * @param errors
     *     The {@link ApiError}s that should represent the exception. This should not be null or empty.
     * @param extraDetailsForLogging
     *     Any extra key/value pairs that should be logged when Backstopper logs the exception/error.
     *
     * @return An instance with {@link #shouldHandleResponse} set to true, {@link #errors} set to a copy of the given
     * set of errors, and {@link #extraDetailsForLogging} set to a copy of the given list of logging data.
     */
    public static ApiExceptionHandlerListenerResult handleResponse(SortedApiErrorSet errors,
                                                                   List<Pair<String, String>> extraDetailsForLogging) {
        return new ApiExceptionHandlerListenerResult(true, errors, extraDetailsForLogging, null);
    }

    /**
     * @param errors
     *     The {@link ApiError}s that should represent the exception. This should not be null or empty.
     * @param extraDetailsForLogging
     *     Any extra key/value pairs that should be logged when Backstopper logs the exception/error.
     * @param extraResponseHeaders
     *     Any extra response headers that the framework should include in the response to the caller.
     *
     * @return An instance with {@link #shouldHandleResponse} set to true, {@link #errors} set to a copy of the given
     * set of errors, and {@link #extraDetailsForLogging} set to a copy of the given list of logging data.
     */
    public static ApiExceptionHandlerListenerResult handleResponse(
        SortedApiErrorSet errors,
        List<Pair<String, String>> extraDetailsForLogging,
        List<Pair<String, List<String>>> extraResponseHeaders
    ) {
        return new ApiExceptionHandlerListenerResult(true, errors, extraDetailsForLogging, extraResponseHeaders);
    }

    /**
     * @return An instance with {@link #shouldHandleResponse} set to false.
     */
    public static ApiExceptionHandlerListenerResult ignoreResponse() {
        return new ApiExceptionHandlerListenerResult(false, null, null, null);
    }
}
