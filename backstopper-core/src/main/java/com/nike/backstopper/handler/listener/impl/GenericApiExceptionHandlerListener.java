package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;
import com.nike.internal.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles generic {@link ApiException} errors by simply setting {@link ApiExceptionHandlerListenerResult#errors} to
 * {@link ApiException#apiErrors} and adding any {@link ApiException#extraDetailsForLogging} and/or
 * {@link ApiException#extraResponseHeaders}.
 */
@Named
@Singleton
public class GenericApiExceptionHandlerListener implements ApiExceptionHandlerListener {
    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        // We only care about ApiExceptions.
        if (!(ex instanceof ApiException))
            return ApiExceptionHandlerListenerResult.ignoreResponse();

        ApiException apiException = ((ApiException)ex);

        // Add all the ApiErrors from the exception.
        SortedApiErrorSet errors = new SortedApiErrorSet();
        errors.addAll(apiException.getApiErrors());

        // Add all the extra details for logging from the exception.
        List<Pair<String, String>> messages = new ArrayList<>();
        messages.addAll(apiException.getExtraDetailsForLogging());

        // Add all the extra response headers from the exception.
        List<Pair<String, List<String>>> headers = new ArrayList<>();
        headers.addAll(apiException.getExtraResponseHeaders());

        // Include the ApiException's message as a logged key/value pair.
        if (StringUtils.isNotBlank(ex.getMessage()))
            messages.add(Pair.of("api_exception_message", ex.getMessage()));

        return ApiExceptionHandlerListenerResult.handleResponse(errors, messages, headers);
    }
}
