package com.nike.backstopper.handler.listener;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.internal.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A data structure representing whether or not a {@link ApiExceptionHandlerListener} wants to handle a specific
 * exception, and if so what the {@link #errors} and {@link #extraDetailsForLogging} should be.
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
     * Intentionally protected - outside callers should use one of the {@link #handleResponse(SortedApiErrorSet)},
     * {@link #handleResponse(SortedApiErrorSet, java.util.List)}, or {@link #ignoreResponse()} static factory methods
     * for generating a new instance.
     */
    protected ApiExceptionHandlerListenerResult(boolean shouldHandleResponse, SortedApiErrorSet errors,
                                                List<Pair<String, String>> extraDetailsForLogging) {
        this.shouldHandleResponse = shouldHandleResponse;
        if (errors == null)
            errors = new SortedApiErrorSet();
        this.errors = new SortedApiErrorSet(errors);
        if (extraDetailsForLogging == null)
            extraDetailsForLogging = new ArrayList<>();
        this.extraDetailsForLogging = new ArrayList<>(extraDetailsForLogging);
    }

    /**
     * @return An instance with {@link #shouldHandleResponse} set to true and {@link #errors} set to a copy of the
     *          given set of errors.
     */
    public static ApiExceptionHandlerListenerResult handleResponse(SortedApiErrorSet errors) {
        return new ApiExceptionHandlerListenerResult(true, errors, null);
    }

    /**
     * @return An instance with {@link #shouldHandleResponse} set to true, {@link #errors} set to a copy of the given
     *          set of errors, and {@link #extraDetailsForLogging} set to a copy of the given list of logging data.
     */
    public static ApiExceptionHandlerListenerResult handleResponse(SortedApiErrorSet errors,
                                                                   List<Pair<String, String>> extraDetailsForLogging) {
        return new ApiExceptionHandlerListenerResult(true, errors, extraDetailsForLogging);
    }

    /**
     * @return An instance with {@link #shouldHandleResponse} set to false.
     */
    public static ApiExceptionHandlerListenerResult ignoreResponse() {
        return new ApiExceptionHandlerListenerResult(false, null, null);
    }
}
