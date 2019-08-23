package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

    // WebExchangeBindException is Spring 5+, but we might be running in Spring 4.
    //      So we have to access WebExchangeBindException.getAllErrors() by reflection. :(
    protected static final String WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME =
        "org.springframework.web.bind.support.WebExchangeBindException";

    protected static final @Nullable Method webExchangeBindExGetAllErrorsMethod;
    
    protected final ProjectApiErrors projectApiErrors;

    static {
        webExchangeBindExGetAllErrorsMethod = extractGetAllErrorsMethod(WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME);
    }

    static @Nullable Method extractGetAllErrorsMethod(String classname) {
        try {
            Class<?> webExchangeBindExClass = Class.forName(classname);
            Method methodToUse = webExchangeBindExClass.getDeclaredMethod("getAllErrors");
            methodToUse.setAccessible(true);
            return methodToUse;
        }
        catch (ClassNotFoundException e) {
            // Do nothing - this is expected when running in Spring 4.
        }
        catch (Exception e) {
            // This should hopefully never happen - but if it does, at least log the error
            //      so the user knows what happened.
            logger.error(
                "Unable to retrieve the getAllErrors() method from the class {}. Backstopper will be unable to "
                + "provide full error details to the user when encountering this exception.",
                classname,
                e
            );
        }

        // We were unable to successfully pull the desired method (for whatever reason), so return null.
        return null;
    }

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

        if (ex instanceof MethodArgumentNotValidException) {
            return ApiExceptionHandlerListenerResult.handleResponse(
                convertSpringErrorsToApiErrors(
                    ((MethodArgumentNotValidException) ex).getBindingResult().getAllErrors()
                )
            );
        }

        if (ex instanceof BindException) {
            return ApiExceptionHandlerListenerResult.handleResponse(
                convertSpringErrorsToApiErrors(((BindException) ex).getAllErrors())
            );
        }

        String exClassname = (ex == null) ? null : ex.getClass().getName();
        if (Objects.equals(exClassname, WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME)) {
            List<ObjectError> objectErrors = extractAllErrorsFromWebExchangeBindException(ex);
            if (objectErrors != null && !objectErrors.isEmpty()) {
                return ApiExceptionHandlerListenerResult.handleResponse(
                    convertSpringErrorsToApiErrors(objectErrors)
                );
            }
        }

        // If we reach here then we didn't handle the exception.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    protected @Nullable List<ObjectError> extractAllErrorsFromWebExchangeBindException(
        @NotNull Throwable ex
    ) {
        Method getAllErrorsMethod = getWebExchangeBindExGetAllErrorsMethod();
        if (getAllErrorsMethod == null) {
            return null;
        }

        try {
            return (List<ObjectError>) getAllErrorsMethod.invoke(ex);
        }
        catch (Exception e) {
            logger.warn(
                "Unexpected error occurred while trying to access WebExchangeBindException.getAllErrors(). "
                + "Backstopper will be unable to provide full error details to the user for this exception.",
                e
            );
            return null;
        }
    }

    protected @Nullable Method getWebExchangeBindExGetAllErrorsMethod() {
        return webExchangeBindExGetAllErrorsMethod;
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
