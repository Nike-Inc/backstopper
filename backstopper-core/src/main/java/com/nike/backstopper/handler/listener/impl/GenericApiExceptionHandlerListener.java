package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;
import com.nike.internal.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Handles generic {@link ApiException} errors by simply setting {@link ApiExceptionHandlerListenerResult#errors} to
 * {@link ApiException#getApiErrors()} and adding any {@link ApiException#getExtraDetailsForLogging()} and/or
 * {@link ApiException#getExtraResponseHeaders()}.
 */
@Named
@Singleton
public class GenericApiExceptionHandlerListener implements ApiExceptionHandlerListener {
    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        // We only care about ApiExceptions.
        if (!(ex instanceof ApiException apiException))
            return ApiExceptionHandlerListenerResult.ignoreResponse();

        // Add all the ApiErrors from the exception.
        SortedApiErrorSet errors = new SortedApiErrorSet();
        errors.addAll(apiException.getApiErrors());

        // Add all the extra details for logging from the exception.
        List<Pair<String, String>> messages = new ArrayList<>(apiException.getExtraDetailsForLogging());

        // Add all the extra response headers from the exception.
        List<Pair<String, List<String>>> headers = new ArrayList<>(apiException.getExtraResponseHeaders());

        // Include the ApiException's message as a logged key/value pair.
        if (StringUtils.isNotBlank(ex.getMessage()))
            messages.add(Pair.of("api_exception_message", ex.getMessage()));

        if (ex.getCause() != null) {
            messages.add(Pair.of("exception_cause_class", ex.getCause().getClass().getName()));
            messages.add(Pair.of("exception_cause_message", ex.getCause().getMessage()));
        }

        return ApiExceptionHandlerListenerResult.handleResponse(errors, messages, headers);
    }
}
