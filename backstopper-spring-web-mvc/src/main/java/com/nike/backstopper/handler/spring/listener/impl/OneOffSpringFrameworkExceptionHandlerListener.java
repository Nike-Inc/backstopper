package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.nike.backstopper.apierror.SortedApiErrorSet.singletonSortedSetOf;

/**
 * Handles the one-off spring framework exceptions that don't fall into any other {@link ApiExceptionHandlerListener}'s
 * domain.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class OneOffSpringFrameworkExceptionHandlerListener implements ApiExceptionHandlerListener {

    protected final ProjectApiErrors projectApiErrors;
    protected final ApiExceptionHandlerUtils utils;

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     *                         ApiError}s. Cannot be null.
     * @param utils            The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass
     *                         in {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    @Inject
    public OneOffSpringFrameworkExceptionHandlerListener(ProjectApiErrors projectApiErrors,
                                                         ApiExceptionHandlerUtils utils) {
        if (projectApiErrors == null) {
            throw new IllegalArgumentException("ProjectApiErrors cannot be null");
        }

        if (utils == null) {
            throw new IllegalArgumentException("ApiExceptionHandlerUtils cannot be null.");
        }

        this.projectApiErrors = projectApiErrors;
        this.utils = utils;
    }

    // NOTE: If you're comparing the exception handling done in this method with Spring's
    //      DefaultHandlerExceptionResolver and/or ResponseEntityExceptionHandler to verify completeness, keep in
    //      mind that MethodArgumentNotValidException and BindException are handled by
    //      ConventionBasedSpringValidationErrorToApiErrorHandlerListener - they should not be handled here.
    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        SortedApiErrorSet handledErrors = null;
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();

        String exClassname = (ex == null) ? null : ex.getClass().getName();

        if (
            ex instanceof NoHandlerFoundException
            // NoSuchRequestHandlingMethodException is a deprecated Spring 4.x exception that doesn't appear in
            //      Spring 5 but should be translated to a 404.
            || Objects.equals(exClassname, "org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException")
        ) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getNotFoundApiError());
        }

        if (ex instanceof TypeMismatchException) {
            TypeMismatchException tme = (TypeMismatchException) ex;
            // The metadata will only be used if it's a 400 error.
            Map<String, Object> metadata = new LinkedHashMap<>();

            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);

            String badPropName = extractPropertyName(tme);
            String badPropValue = (tme.getValue() == null) ? null : String.valueOf(tme.getValue());
            String requiredTypeNoInfoLeak = extractRequiredTypeNoInfoLeak(tme);

            extraDetailsForLogging.add(Pair.of("bad_property_name", badPropName));
            if (badPropName != null) {
                metadata.put("bad_property_name", badPropName);
            }
            extraDetailsForLogging.add(Pair.of("bad_property_value", String.valueOf(tme.getValue())));
            if (badPropValue != null) {
                metadata.put("bad_property_value", badPropValue);
            }
            extraDetailsForLogging.add(Pair.of("required_type", String.valueOf(tme.getRequiredType())));
            if (requiredTypeNoInfoLeak != null) {
                metadata.put("required_type", requiredTypeNoInfoLeak);
            }

            // ConversionNotSupportedException is a special case of TypeMismatchException that should be treated as
            //      a 500 internal service error. See Spring's DefaultHandlerExceptionResolver and/or
            //      ResponseEntityExceptionHandler for verification.
            if (ex instanceof ConversionNotSupportedException) {
                // We can add even more context log details if it's a MethodArgumentConversionNotSupportedException.
                if (ex instanceof MethodArgumentConversionNotSupportedException) {
                    MethodArgumentConversionNotSupportedException macnsEx = (MethodArgumentConversionNotSupportedException)ex;
                    extraDetailsForLogging.add(Pair.of("method_arg_name", macnsEx.getName()));
                    extraDetailsForLogging.add(Pair.of("method_arg_target_param", macnsEx.getParameter().toString()));
                }

                handledErrors = singletonSortedSetOf(projectApiErrors.getGenericServiceError());
            }
            else {
                // All other TypeMismatchExceptions should be treated as a 400, and we can/should include the metadata.

                // We can add even more context log details if it's a MethodArgumentTypeMismatchException.
                if (ex instanceof MethodArgumentTypeMismatchException) {
                    MethodArgumentTypeMismatchException matmEx = (MethodArgumentTypeMismatchException)ex;
                    extraDetailsForLogging.add(Pair.of("method_arg_name", matmEx.getName()));
                    extraDetailsForLogging.add(Pair.of("method_arg_target_param", matmEx.getParameter().toString()));
                }
                
                handledErrors = singletonSortedSetOf(
                    new ApiErrorWithMetadata(projectApiErrors.getTypeConversionApiError(), metadata)
                );
            }
        }

        if (ex instanceof ServletRequestBindingException) {
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

            handledErrors = singletonSortedSetOf(errorToUse);
        }

        if (ex instanceof HttpMessageConversionException) {
            // Malformed requests can be difficult to track down - add the exception's message to our logging details
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);

            // HttpMessageNotWritableException is a special case of HttpMessageConversionException that should be
            //      treated as a 500 internal service error. See Spring's DefaultHandlerExceptionResolver and/or
            //      ResponseEntityExceptionHandler for verification.
            if (ex instanceof HttpMessageNotWritableException) {
                handledErrors = singletonSortedSetOf(projectApiErrors.getGenericServiceError());
            }
            else {
                // All other HttpMessageConversionException should be treated as a 400.
                if (isMissingExpectedContentCase((HttpMessageConversionException) ex)) {
                    handledErrors = singletonSortedSetOf(projectApiErrors.getMissingExpectedContentApiError());
                }
                else {
                    // NOTE: If this was a HttpMessageNotReadableException with a cause of
                    //          com.fasterxml.jackson.databind.exc.InvalidFormatException then we *could* theoretically map
                    //          to projectApiErrors.getTypeConversionApiError(). If we ever decide to implement this, then
                    //          InvalidFormatException does contain reference to the field that failed to convert - we can
                    //          get to it via getPath(), iterating over each path object, and building the full path by
                    //          concatenating them with '.'. For now we'll just turn all errors in this category into
                    //          projectApiErrors.getMalformedRequestApiError().
                    handledErrors = singletonSortedSetOf(projectApiErrors.getMalformedRequestApiError());
                }
            }
        }

        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getNoAcceptableRepresentationApiError());
        }

        if (ex instanceof HttpMediaTypeNotSupportedException) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getUnsupportedMediaTypeApiError());
        }

        if (ex instanceof HttpRequestMethodNotSupportedException) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getMethodNotAllowedApiError());
        }

        if (ex instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException detailsEx = (MissingServletRequestPartException)ex;
            handledErrors = singletonSortedSetOf(
                new ApiErrorWithMetadata(
                    projectApiErrors.getMalformedRequestApiError(),
                    Pair.of("missing_required_part", (Object)detailsEx.getRequestPartName())
                )
            );
        }

        // AsyncRequestTimeoutException didn't show up until more recent versions of Spring, so we'll check for it
        //      by classname to prevent unnecessarily breaking existing Backstopper users on older versions of Spring.
        if (Objects.equals(exClassname, "org.springframework.web.context.request.async.AsyncRequestTimeoutException")) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getTemporaryServiceProblemApiError());
        }

        if (handledErrors != null) {
            return ApiExceptionHandlerListenerResult.handleResponse(handledErrors, extraDetailsForLogging);
        }

        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    protected boolean isMissingExpectedContentCase(HttpMessageConversionException ex) {
        if (ex instanceof HttpMessageNotReadableException) {
            // Different versions of Spring Web MVC can have different ways of expressing missing content.

            // More common case
            if (ex.getMessage().startsWith("Required request body is missing")) {
                return true;
            }

            // An older/more unusual case. Unfortunately there's a lot of manual digging that we have to do to determine
            //      that we've reached this case.
            //noinspection RedundantIfStatement
            if (ex.getCause() != null && ex.getCause() instanceof JsonMappingException
                && ex.getCause().getMessage() != null && ex.getCause().getMessage()
                                                           .contains("No content to map due to end-of-input")) {
                return true;
            }
        }

        return false;
    }

    protected String extractPropertyName(TypeMismatchException tme) {
        if (tme instanceof MethodArgumentTypeMismatchException) {
            return ((MethodArgumentTypeMismatchException) tme).getName();
        }

        if (tme instanceof MethodArgumentConversionNotSupportedException) {
            return ((MethodArgumentConversionNotSupportedException) tme).getName();
        }

        return null;
    }

    protected String extractRequiredTypeNoInfoLeak(TypeMismatchException tme) {
        if (tme.getRequiredType() == null) {
            return null;
        }

        if (isRequiredTypeAssignableToOneOf(tme, Byte.class, byte.class)) {
            return "byte";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Short.class, short.class)) {
            return "short";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Integer.class, int.class)) {
            return "int";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Long.class, long.class)) {
            return "long";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Float.class, float.class)) {
            return "float";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Double.class, double.class)) {
            return "double";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Boolean.class, boolean.class)) {
            return "boolean";
        }

        if (isRequiredTypeAssignableToOneOf(tme, Character.class, char.class)) {
            return "char";
        }

        if (isRequiredTypeAssignableToOneOf(tme, CharSequence.class)) {
            return "string";
        }

        return "[complex type]";
    }

    protected boolean isRequiredTypeAssignableToOneOf(TypeMismatchException tme, Class<?>... allowedClasses) {
        Class<?> desiredClass = tme.getRequiredType();
        for (Class<?> allowedClass : allowedClasses) {
            if (allowedClass.isAssignableFrom(desiredClass)) {
                return true;
            }
        }

        return false;
    }
}
