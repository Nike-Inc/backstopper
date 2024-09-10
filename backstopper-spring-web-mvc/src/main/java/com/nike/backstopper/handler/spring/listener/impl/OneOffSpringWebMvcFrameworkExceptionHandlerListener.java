package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * An extension and concrete implementation of {@link OneOffSpringCommonFrameworkExceptionHandlerListener} that
 * knows how to handle Spring WebMVC specific exceptions.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class OneOffSpringWebMvcFrameworkExceptionHandlerListener
    extends OneOffSpringCommonFrameworkExceptionHandlerListener {

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     * ApiError}s. Cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     * {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    @Inject
    public OneOffSpringWebMvcFrameworkExceptionHandlerListener(ProjectApiErrors projectApiErrors,
                                                               ApiExceptionHandlerUtils utils) {
        super(projectApiErrors, utils);
    }

    @Override
    protected @NotNull ApiExceptionHandlerListenerResult handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
        @NotNull Throwable ex
    ) {
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        
        if (ex instanceof ServletRequestBindingException) {
            return handleServletRequestBindingException((ServletRequestBindingException)ex, extraDetailsForLogging);
        }

        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            return handleError(projectApiErrors.getNoAcceptableRepresentationApiError(), extraDetailsForLogging);
        }

        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return handleError(projectApiErrors.getUnsupportedMediaTypeApiError(), extraDetailsForLogging);
        }

        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return handleError(projectApiErrors.getMethodNotAllowedApiError(), extraDetailsForLogging);
        }

        if (ex instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException detailsEx = (MissingServletRequestPartException)ex;
            return handleError(
                new ApiErrorWithMetadata(
                    projectApiErrors.getMalformedRequestApiError(),
                    Pair.of("missing_required_part", (Object)detailsEx.getRequestPartName())
                ),
                extraDetailsForLogging
            );
        }

        // This exception is not handled here.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    protected ApiExceptionHandlerListenerResult handleServletRequestBindingException(
        ServletRequestBindingException ex,
        List<Pair<String, String>> extraDetailsForLogging
    ) {
        // Malformed requests can be difficult to track down - add the exception's message to our logging details
        utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);

        ApiError errorToUse = projectApiErrors.getMalformedRequestApiError();

        // Add some extra context metadata if it's a MissingServletRequestParameterException.
        if (ex instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException detailsEx = (MissingServletRequestParameterException)ex;
            
            errorToUse = new ApiErrorWithMetadata(
                errorToUse,
                Pair.of("missing_param_name", (Object)detailsEx.getParameterName()),
                Pair.of("missing_param_type", (Object)detailsEx.getParameterType())
            );
        }

        return handleError(errorToUse, extraDetailsForLogging);
    }
}
