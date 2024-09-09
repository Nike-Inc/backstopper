package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.exception.ServersideValidationError;
import com.nike.backstopper.exception.network.DownstreamRequestOrResponseBodyFailedValidationException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;

import static com.nike.backstopper.apierror.SortedApiErrorSet.singletonSortedSetOf;

/**
 * Handles {@link ServersideValidationError} exceptions by adding the relevant logging info useful for debugging to the
 * returned {@link ApiExceptionHandlerListenerResult#extraDetailsForLogging} and setting the returned
 * {@link ApiExceptionHandlerListenerResult#errors} to {@link ProjectApiErrors#getServersideValidationApiError()}.
 *
 * <p>NOTE: This class also handles {@link DownstreamRequestOrResponseBodyFailedValidationException} exceptions the same
 * way if the exception's cause is a {@link ServersideValidationError}. It will just extract the wrapped
 * {@link ServersideValidationError} and use that as if it was passed in directly.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class ServersideValidationErrorHandlerListener implements ApiExceptionHandlerListener {

    protected final ProjectApiErrors projectApiErrors;
    protected final ApiExceptionHandlerUtils utils;

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding
     *                          {@link ApiError}s. Cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     *              {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    @Inject
    public ServersideValidationErrorHandlerListener(ProjectApiErrors projectApiErrors,
                                                    ApiExceptionHandlerUtils utils) {
        if (projectApiErrors == null)
            throw new IllegalArgumentException("ProjectApiErrors cannot be null");

        if (utils == null)
            throw new IllegalArgumentException("apiExceptionHandlerUtils cannot be null.");

        this.projectApiErrors = projectApiErrors;
        this.utils = utils;
    }

    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        ServersideValidationError sve = null;
        // Try to get the ServersideValidationError
        if (ex instanceof ServersideValidationError)
            sve = (ServersideValidationError)ex;
        else if ((ex instanceof DownstreamRequestOrResponseBodyFailedValidationException)
                 && (ex.getCause() instanceof ServersideValidationError)) {
            sve = (ServersideValidationError) ex.getCause();
        }

        if (sve != null) {
            // Process the ServersideValidationError to add logging details and get the appropriate SortedApiErrorSet.
            List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
            SortedApiErrorSet apiErrors = processServersideValidationError(sve, extraDetailsForLogging);
            return ApiExceptionHandlerListenerResult.handleResponse(apiErrors, extraDetailsForLogging);
        }

        // Not an exception we know how to handle - ignore it.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    /**
     * @return A {@link SortedApiErrorSet} containing {@link ProjectApiErrors#getServersideValidationApiError()}
     *          after populating the extraDetailsForLogging with the relevant info from the exception.
     */
    protected SortedApiErrorSet processServersideValidationError(ServersideValidationError ex,
                                                                 List<Pair<String, String>> extraDetailsForLogging) {
        // Add info about the object that failed validation.
        if (ex.getObjectThatFailedValidation() != null)
            extraDetailsForLogging.add(
                Pair.of("serverside_validation_object", ex.getObjectThatFailedValidation().getClass().getName())
            );

        // Add info about each violation that occurred.
        if (ex.getViolations() != null) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<Object> violation : ex.getViolations()) {
                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(violation.getPropertyPath().toString())
                  .append("|") .append(violation.getConstraintDescriptor().getAnnotation().annotationType().getName())
                  .append("|").append(violation.getMessage());
            }
            extraDetailsForLogging.add(
                Pair.of("serverside_validation_errors", utils.quotesToApostrophes(sb.toString()))
            );
        }

        return singletonSortedSetOf(projectApiErrors.getServersideValidationApiError());
    }
}
