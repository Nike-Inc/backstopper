package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Handles exceptions from the Spring framework that represent JSR 303 validation errors. This handler assumes you're
 * following the convention where all JSR 303 error messages can be converted to {@link ApiError} (see {@link
 * #convertSpringErrorToApiError(ObjectError)}).
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class ConventionBasedSpringValidationErrorToApiErrorHandlerListener implements ApiExceptionHandlerListener {

    private static final Logger logger =
        LoggerFactory.getLogger(ConventionBasedSpringValidationErrorToApiErrorHandlerListener.class);

    protected final ProjectApiErrors projectApiErrors;

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     *                         ApiError}s. Cannot be null.
     */
    @Inject
    public ConventionBasedSpringValidationErrorToApiErrorHandlerListener(
        @NotNull ProjectApiErrors projectApiErrors
    ) {
        //noinspection ConstantConditions
        if (projectApiErrors == null) {
            throw new IllegalArgumentException("ProjectApiErrors cannot be null");
        }

        this.projectApiErrors = projectApiErrors;
    }

    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {

        if (ex instanceof Errors errEx) {
            List<ObjectError> errList = errEx.getAllErrors();
            //noinspection ConstantValue
            if (errList != null && !errList.isEmpty()) {
                return ApiExceptionHandlerListenerResult.handleResponse(
                    convertSpringErrorsToApiErrors(errList)
                );
            }
        }

        // If we reach here then we didn't handle the exception.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    /**
     * Helper method for translating the given springErrors set into a set of {@link ApiError} objects by calling {@link
     * #convertSpringErrorToApiError(ObjectError)} on each one.
     */
    protected SortedApiErrorSet convertSpringErrorsToApiErrors(List<ObjectError> springErrors) {
        SortedApiErrorSet apiErrors = new SortedApiErrorSet();
        for (ObjectError springError : springErrors) {
            apiErrors.add(convertSpringErrorToApiError(springError));
        }

        return apiErrors;
    }

    /**
     * @return Converts the given springError to a {@link ApiError} if the springError's {@link
     *          ObjectError#getDefaultMessage()} corresponds to a {@link ApiError} value using
     *          {@link ProjectApiErrors#convertToApiError(String)}, otherwise falls back to {@link
     *          ProjectApiErrors#getGenericServiceError()}.
     */
    protected ApiError convertSpringErrorToApiError(ObjectError springError) {
        String message = springError.getDefaultMessage();
        ApiError apiError = projectApiErrors.convertToApiError(message);
        if (apiError == null)
            return projectApiErrors.getGenericServiceError();

        if (springError instanceof FieldError) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("field", ((FieldError)springError).getField());
            apiError = new ApiErrorWithMetadata(apiError, metadata);
        }

        return apiError;
    }

}
