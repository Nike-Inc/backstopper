package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nike.backstopper.apierror.SortedApiErrorSet.singletonSortedSetOf;
import static java.util.Collections.singleton;

/**
 * Handles the one-off spring framework exceptions common to any spring environment (e.g. WebMVC and WebFlux) that
 * don't fall into any other {@link ApiExceptionHandlerListener}'s domain.
 *
 * <p>The exceptions handled here are primarily ones that are found in the spring-web dependency as that's common to
 * both WebMVC and WebFlux apps, however this also contains support for Spring Security exceptions via simply checking
 * classnames, and a few spring-webmvc exceptions that we can also handle by checking classname (so we don't need the
 * spring-security or spring-webmvc dependencies in this backstopper-spring-web module). Also note that a few
 * exceptions in spring-web extend from Servlet API exceptions. Those are not handled here since we don't want a
 * servlet-api dependency included. Instead they are handled by the backstopper-spring-web-mvc's
 * {@code OneOffSpringWebMvcFrameworkExceptionHandlerListener} class.
 *
 * <p>NOTE: This class is abstract - concrete implementations must implement
 * {@link #handleSpringMvcOrWebfluxSpecificFrameworkExceptions(Throwable)} to handle the Spring WebMVC or WebFlux
 * exceptions relevant to the specific Spring environment flavor they're covering.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class OneOffSpringCommonFrameworkExceptionHandlerListener implements ApiExceptionHandlerListener {

    protected final ProjectApiErrors projectApiErrors;
    protected final ApiExceptionHandlerUtils utils;

    // Support all the various 404 cases from competing dependencies using classname matching.
    protected final Set<String> DEFAULT_TO_404_CLASSNAMES = new LinkedHashSet<>(Arrays.asList(
        // NoHandlerFoundException is found in the spring-webmvc dependency, not spring-web.
        "org.springframework.web.servlet.NoHandlerFoundException"
    ));

    // Support Spring Security exceptions that should map to a 403.
    protected final Set<String> DEFAULT_TO_403_CLASSNAMES = singleton(
        "org.springframework.security.access.AccessDeniedException"
    );

    // Support Spring Security exceptions that should map to a 401.
    protected final Set<String> DEFAULT_TO_401_CLASSNAMES = new LinkedHashSet<>(Arrays.asList(
        "org.springframework.security.authentication.BadCredentialsException",
        "org.springframework.security.authentication.InsufficientAuthenticationException",
        "org.springframework.security.authentication.AuthenticationCredentialsNotFoundException",
        "org.springframework.security.authentication.LockedException",
        "org.springframework.security.authentication.DisabledException",
        "org.springframework.security.authentication.CredentialsExpiredException",
        "org.springframework.security.authentication.AccountExpiredException",
        "org.springframework.security.core.userdetails.UsernameNotFoundException"
    ));

    // Support 503 cases from competing dependencies using classname matching.
    protected final Set<String> DEFAULT_TO_503_CLASSNAMES = singleton(
        // AsyncRequestTimeoutException didn't show up until more recent versions of Spring, so we'll check for it
        //      by classname to prevent unnecessarily breaking existing Backstopper users on older versions of Spring.
        "org.springframework.web.context.request.async.AsyncRequestTimeoutException"
    );

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     *                         ApiError}s. Cannot be null.
     * @param utils            The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass
     *                         in {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    public OneOffSpringCommonFrameworkExceptionHandlerListener(ProjectApiErrors projectApiErrors,
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

    protected abstract @NotNull ApiExceptionHandlerListenerResult handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
        @NotNull Throwable ex
    );

    // NOTE: If you're comparing the exception handling done in this method with Spring's
    //      DefaultHandlerExceptionResolver and/or ResponseEntityExceptionHandler to verify completeness, keep in
    //      mind that MethodArgumentNotValidException and BindException are handled by
    //      ConventionBasedSpringValidationErrorToApiErrorHandlerListener - they should not be handled here.
    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        if (ex == null) {
            // This probably can't happen in reality, but if it does there's nothing for us to handle.
            return ApiExceptionHandlerListenerResult.ignoreResponse();
        }

        // See if it's a Spring MVC or WebFlux specific exception first.
        ApiExceptionHandlerListenerResult mvcOrWebfluxSpecificHandlerResult =
            handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        if (mvcOrWebfluxSpecificHandlerResult.shouldHandleResponse) {
            return mvcOrWebfluxSpecificHandlerResult;
        }

        // Not a Spring MVC or WebFlux specific exception. See if it's an exception common to both.
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();

        if (ex instanceof ResponseStatusException) {
            return handleResponseStatusException((ResponseStatusException)ex);
        }

        String exClassname = ex.getClass().getName();

        if (isA404NotFoundExceptionClassname(exClassname)) {
            return handleError(projectApiErrors.getNotFoundApiError(), extraDetailsForLogging);
        }

        if (ex instanceof TypeMismatchException) {
            return handleTypeMismatchException((TypeMismatchException)ex, extraDetailsForLogging, true, null);
        }

        if (ex instanceof HttpMessageConversionException) {
            return handleHttpMessageConversionException((HttpMessageConversionException)ex, extraDetailsForLogging);
        }

        if (isA503TemporaryProblemExceptionClassname(exClassname)) {
            return handleError(projectApiErrors.getTemporaryServiceProblemApiError(), extraDetailsForLogging);
        }

        if (isA401UnauthorizedExceptionClassname(exClassname)) {
            return handleError(projectApiErrors.getUnauthorizedApiError(), extraDetailsForLogging);
        }

        if (isA403ForibddenExceptionClassname(exClassname)) {
            return handleError(projectApiErrors.getForbiddenApiError(), extraDetailsForLogging);
        }

        // This exception is not handled here.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    protected ApiExceptionHandlerListenerResult handleError(
        ApiError error,
        List<Pair<String, String>> extraDetailsForLogging
    ) {
        return ApiExceptionHandlerListenerResult.handleResponse(singletonSortedSetOf(error), extraDetailsForLogging);
    }

    protected ApiExceptionHandlerListenerResult handleHttpMessageConversionException(
        HttpMessageConversionException ex,
        List<Pair<String, String>> extraDetailsForLogging
    ) {
        // Malformed requests can be difficult to track down - add the exception's message to our logging details
        utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);

        // HttpMessageNotWritableException is a special case of HttpMessageConversionException that should be
        //      treated as a 500 internal service error. See Spring's DefaultHandlerExceptionResolver and/or
        //      ResponseEntityExceptionHandler for verification.
        if (ex instanceof HttpMessageNotWritableException) {
            return handleError(projectApiErrors.getGenericServiceError(), extraDetailsForLogging);
        }
        else {
            // All other HttpMessageConversionException should be treated as a 400.
            if (isMissingExpectedContentCase(ex)) {
                return handleError(projectApiErrors.getMissingExpectedContentApiError(), extraDetailsForLogging);
            }
            else {
                // NOTE: If this was a HttpMessageNotReadableException with a cause of
                //          com.fasterxml.jackson.databind.exc.InvalidFormatException then we *could* theoretically map
                //          to projectApiErrors.getTypeConversionApiError(). If we ever decide to implement this, then
                //          InvalidFormatException does contain reference to the field that failed to convert - we can
                //          get to it via getPath(), iterating over each path object, and building the full path by
                //          concatenating them with '.'. For now we'll just turn all errors in this category into
                //          projectApiErrors.getMalformedRequestApiError().
                return handleError(projectApiErrors.getMalformedRequestApiError(), extraDetailsForLogging);
            }
        }
    }

    protected boolean isMissingExpectedContentCase(HttpMessageConversionException ex) {
        if (ex instanceof HttpMessageNotReadableException) {
            // Different versions of Spring Web MVC and underlying deserializers (e.g. Jackson) can have different ways of expressing missing content.

            // A common case.
            if (ex.getMessage().startsWith("Required request body is missing")) {
                return true;
            }

            // Underlying Jackson cases. Unfortunately there's a lot of manual digging that we have to do to determine
            //      that we've reached these cases.
            Throwable cause = ex.getCause();
            if (cause != null) {
                String causeClassName = cause.getClass().getName();
                if ("com.fasterxml.jackson.databind.exc.InvalidFormatException".equals(causeClassName)
                    && nullSafeStringContains(cause.getMessage(), "Cannot coerce empty String")
                ) {
                    return true;
                }

                //noinspection RedundantIfStatement
                if ("com.fasterxml.jackson.databind.JsonMappingException".equals(causeClassName)
                    && nullSafeStringContains(cause.getMessage(), "No content to map due to end-of-input")
                ) {
                    return true;
                }
            }
        }

        return false;
    }
    
    protected boolean nullSafeStringContains(String strToCheck, String snippet) {
        if (strToCheck == null || snippet == null) {
            return false;
        }

        return strToCheck.contains(snippet);
    }

    protected ApiExceptionHandlerListenerResult handleTypeMismatchException(
        TypeMismatchException ex,
        List<Pair<String, String>> extraDetailsForLogging,
        boolean addBaseExceptionMessageToLoggingDetails,
        @Nullable List<Pair<String, String>> extraMetadata
    ) {
        // The metadata will only be used if it's a 400 error.
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (extraMetadata != null) {
            for (Pair<String, String> pair : extraMetadata) {
                if (pair != null) {
                    metadata.put(pair.getKey(), pair.getValue());
                }
            }
        }

        if (addBaseExceptionMessageToLoggingDetails) {
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
        }

        String badPropName = extractPropertyName(ex);
        if (badPropName == null) {
            badPropName = ex.getPropertyName();
        }
        String badPropValue = (ex.getValue() == null) ? null : String.valueOf(ex.getValue());
        String requiredTypeNoInfoLeak = extractRequiredTypeNoInfoLeak(ex.getRequiredType());

        extraDetailsForLogging.add(Pair.of("bad_property_name", badPropName));
        if (badPropName != null) {
            metadata.put("bad_property_name", badPropName);
        }
        extraDetailsForLogging.add(Pair.of("bad_property_value", String.valueOf(ex.getValue())));
        if (badPropValue != null) {
            metadata.put("bad_property_value", badPropValue);
        }
        extraDetailsForLogging.add(Pair.of("required_type", String.valueOf(ex.getRequiredType())));
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

            return handleError(projectApiErrors.getGenericServiceError(), extraDetailsForLogging);
        }
        else {
            // All other TypeMismatchExceptions should be treated as a 400, and we can/should include the metadata.

            // We can add even more context log details if it's a MethodArgumentTypeMismatchException.
            if (ex instanceof MethodArgumentTypeMismatchException) {
                MethodArgumentTypeMismatchException matmEx = (MethodArgumentTypeMismatchException)ex;
                List<Pair<String, String>> hOrQMetadata = extractExtraMetadataForHeaderOrQueryParamException(matmEx);
                if (hOrQMetadata != null) {
                    for (Pair<String, String> pair : hOrQMetadata) {
                        if (pair != null) {
                            metadata.put(pair.getKey(), pair.getValue());
                        }
                    }
                }
                extraDetailsForLogging.add(Pair.of("method_arg_name", matmEx.getName()));
                extraDetailsForLogging.add(Pair.of("method_arg_target_param", matmEx.getParameter().toString()));
            }

            return handleError(
                new ApiErrorWithMetadata(projectApiErrors.getTypeConversionApiError(), metadata),
                extraDetailsForLogging
            );
        }
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

    protected String extractRequiredTypeNoInfoLeak(Class<?> type) {
        if (type == null) {
            return null;
        }

        if (isRequiredTypeAssignableToOneOf(type, Byte.class, byte.class)) {
            return "byte";
        }

        if (isRequiredTypeAssignableToOneOf(type, Short.class, short.class)) {
            return "short";
        }

        if (isRequiredTypeAssignableToOneOf(type, Integer.class, int.class)) {
            return "int";
        }

        if (isRequiredTypeAssignableToOneOf(type, Long.class, long.class)) {
            return "long";
        }

        if (isRequiredTypeAssignableToOneOf(type, Float.class, float.class)) {
            return "float";
        }

        if (isRequiredTypeAssignableToOneOf(type, Double.class, double.class)) {
            return "double";
        }

        if (isRequiredTypeAssignableToOneOf(type, Boolean.class, boolean.class)) {
            return "boolean";
        }

        if (isRequiredTypeAssignableToOneOf(type, Character.class, char.class)) {
            return "char";
        }

        if (isRequiredTypeAssignableToOneOf(type, CharSequence.class)) {
            return "string";
        }

        return "[complex type]";
    }

    protected boolean isRequiredTypeAssignableToOneOf(Class<?> desiredClass, Class<?>... allowedClasses) {
        for (Class<?> allowedClass : allowedClasses) {
            if (allowedClass.isAssignableFrom(desiredClass)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isA404NotFoundExceptionClassname(String exClassname) {
        return DEFAULT_TO_404_CLASSNAMES.contains(exClassname);
    }

    protected boolean isA403ForibddenExceptionClassname(String exClassname) {
        return DEFAULT_TO_403_CLASSNAMES.contains(exClassname);
    }

    protected boolean isA401UnauthorizedExceptionClassname(String exClassname) {
        return DEFAULT_TO_401_CLASSNAMES.contains(exClassname);
    }

    protected boolean isA503TemporaryProblemExceptionClassname(String exClassname) {
        return DEFAULT_TO_503_CLASSNAMES.contains(exClassname);
    }

    protected @NotNull ApiExceptionHandlerListenerResult handleResponseStatusException(
        @NotNull ResponseStatusException ex
    ) {
        int statusCode = ex.getStatusCode().value();
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
        addExtraDetailsForLoggingForResponseStatusException(ex, extraDetailsForLogging);

        // TODO: Handle HandlerMethodValidationException, which was introduced in spring 6.1,
        //       which means we'd have to do nasty reflection to deal with it.

        // Search for a more specific way to handle this based on the cause.
        Throwable exCause = ex.getCause();
        if (exCause instanceof TypeMismatchException) {
            // If the cause is a TypeMismatchException and status code is acceptable, then we can have the
            //      handleTypeMismatchException(...) method deal with it for a more specific response.

            // For safety make sure the status code is one we expect.
            TypeMismatchException tmeCause = (TypeMismatchException) ex.getCause();
            int expectedStatusCode = (tmeCause instanceof ConversionNotSupportedException) ? 500 : 400;
            if (statusCode == expectedStatusCode) {
                // The specific cause exception type and the status code match,
                //      so we can use handleTypeMismatchException(...).
                return handleTypeMismatchException(
                    tmeCause,
                    extraDetailsForLogging,
                    false,
                    extractExtraMetadataForHeaderOrQueryParamException(ex)
                );
            }
        }
        else if (exCause instanceof DecodingException && statusCode == 400) {
            if (Objects.equals(ex.getReason(), "No request body") || exCause.getMessage().startsWith("No request body for:")) {
                return handleError(projectApiErrors.getMissingExpectedContentApiError(), extraDetailsForLogging);
            }

            return handleError(projectApiErrors.getMalformedRequestApiError(), extraDetailsForLogging);
        }

        // Exception cause didn't help. Try parsing the reason message.
        String exReason = (ex.getReason() == null) ? "" : ex.getReason();
        String[] exReasonWords = exReason.split(" ");

        RequiredParamData missingRequiredParam = parseExReasonForMissingRequiredParam(ex, exReasonWords, exReason);
        if (missingRequiredParam != null && statusCode == 400) {
            return handleError(
                new ApiErrorWithMetadata(
                    projectApiErrors.getMalformedRequestApiError(),
                    missingRequiredParam.getAsApiErrorMetadata()
                ),
                extraDetailsForLogging
            );
        }
        else if (exReason.startsWith("Request body is missing") && statusCode == 400) {
            return handleError(projectApiErrors.getMissingExpectedContentApiError(), extraDetailsForLogging);
        }

        // For any other ResponseStatusException we'll search for an appropriate ApiError by status code.
        return handleError(
            determineApiErrorToUseForGenericResponseStatusCode(statusCode),
            extraDetailsForLogging
        );
    }

    protected @Nullable List<Pair<String, String>> extractExtraMetadataForHeaderOrQueryParamException(
        Exception maybeMethodParamEx
    ) {
        MethodParameter methodParam = null;
        if (maybeMethodParamEx instanceof ServerWebInputException swiEx) {
            methodParam = swiEx.getMethodParameter();
        } else if (maybeMethodParamEx instanceof MethodArgumentTypeMismatchException matmEx) {
            methodParam = matmEx.getParameter();
        }

        if (methodParam == null) {
            return null;
        }

        boolean isHeader = methodParam.hasParameterAnnotation(RequestHeader.class);
        boolean isQueryParam = methodParam.hasParameterAnnotation(RequestParam.class);

        if (isHeader && isQueryParam) {
            return Collections.singletonList(Pair.of("required_location", "header,query_param"));
        }
        else if (isHeader) {
            return Collections.singletonList(Pair.of("required_location", "header"));
        }
        else if (isQueryParam) {
            return Collections.singletonList(Pair.of("required_location", "query_param"));
        }

        return null;
    }

    protected void addExtraDetailsForLoggingForResponseStatusException(
        @NotNull ResponseStatusException ex,
        @NotNull List<Pair<String, String>> extraDetailsForLogging
    ) {
        if (ex instanceof MethodNotAllowedException) {
            MethodNotAllowedException detailsEx = (MethodNotAllowedException)ex;
            extraDetailsForLogging.add(
                Pair.of("supported_methods", concatenateCollectionToString(detailsEx.getSupportedMethods()))
            );
        }

        if (ex instanceof NotAcceptableStatusException) {
            NotAcceptableStatusException detailsEx = (NotAcceptableStatusException)ex;
            extraDetailsForLogging.add(
                Pair.of("supported_media_types", concatenateCollectionToString(detailsEx.getSupportedMediaTypes()))
            );
        }

        if (ex instanceof ServerErrorException) {
            ServerErrorException detailsEx = (ServerErrorException)ex;
            extraDetailsForLogging.add(
                Pair.of("method_parameter", String.valueOf(detailsEx.getMethodParameter()))
            );
            extraDetailsForLogging.add(
                Pair.of("handler_method", String.valueOf(detailsEx.getHandlerMethod()))
            );
        }

        if (ex instanceof ServerWebInputException) {
            ServerWebInputException detailsEx = (ServerWebInputException)ex;
            extraDetailsForLogging.add(
                Pair.of("method_parameter", String.valueOf(detailsEx.getMethodParameter()))
            );
        }

        if (ex instanceof UnsupportedMediaTypeStatusException) {
            UnsupportedMediaTypeStatusException detailsEx = (UnsupportedMediaTypeStatusException)ex;
            extraDetailsForLogging.add(
                Pair.of("supported_media_types", concatenateCollectionToString(detailsEx.getSupportedMediaTypes()))
            );
            extraDetailsForLogging.add(Pair.of("java_body_type", String.valueOf(detailsEx.getBodyType())));
        }
    }

    protected @NotNull String concatenateCollectionToString(@Nullable Collection<?> collection) {
        if (collection == null) {
            return "";
        }
        return collection.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    protected @NotNull ApiError determineApiErrorToUseForGenericResponseStatusCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> projectApiErrors.getGenericBadRequestApiError();
            case 401 -> projectApiErrors.getUnauthorizedApiError();
            case 403 -> projectApiErrors.getForbiddenApiError();
            case 404 -> projectApiErrors.getNotFoundApiError();
            case 405 -> projectApiErrors.getMethodNotAllowedApiError();
            case 406 -> projectApiErrors.getNoAcceptableRepresentationApiError();
            case 415 -> projectApiErrors.getUnsupportedMediaTypeApiError();
            case 429 -> projectApiErrors.getTooManyRequestsApiError();
            case 500 -> projectApiErrors.getGenericServiceError();
            case 503 -> projectApiErrors.getTemporaryServiceProblemApiError();
            default ->
                // If we reach here then it wasn't a status code where we have a common ApiError in ProjectApiErrors.
                //      Generate a generic ApiError to cover it.
                generateGenericApiErrorForResponseStatusCode(statusCode);
        };
    }

    protected @NotNull ApiError generateGenericApiErrorForResponseStatusCode(int statusCode) {
        // Reuse the error code for the generic bad request ApiError, unless the status code is greater than or equal
        //      to 500. If status code >= 500, then use the generic service error status code instead.
        String errorCodeToUse = projectApiErrors.getGenericBadRequestApiError().getErrorCode();
        if (statusCode >= 500) {
            errorCodeToUse = projectApiErrors.getGenericServiceError().getErrorCode();
        }

        return new ApiErrorBase(
            "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_" + statusCode,
            errorCodeToUse,
            "An error occurred that resulted in response status code " + statusCode,
            statusCode
        );
    }

    protected @Nullable RequiredParamData parseExReasonForMissingRequiredParam(
        @NotNull ResponseStatusException ex, @NotNull String[] exReasonWords, @NotNull String exReason
    ) {
        // Check for an exception type where we can get the info without parsing strings.
        if (ex instanceof MissingRequestValueException) {
            MissingRequestValueException detailsEx = (MissingRequestValueException)ex;
            return new RequiredParamData(
                detailsEx.getName(),
                extractRequiredTypeNoInfoLeak(detailsEx.getType()),
                extractExtraMetadataForHeaderOrQueryParamException(ex)
            );
        }

        if (exReasonWords.length != 7) {
            return null;
        }

        if ("Required".equals(exReasonWords[0])
            && "parameter".equals(exReasonWords[2])
            && (exReason.endsWith("is not present.") || exReason.endsWith("is not present"))
        ) {
            return new RequiredParamData(exReasonWords[3].replace("'", ""), exReasonWords[1], null);
        }

        return null;
    }

    protected static class RequiredParamData {
        public final String paramName;
        public final String paramType;
        public final List<Pair<String, String>> extraMetadata;

        public RequiredParamData(String paramName, String paramType, List<Pair<String, String>> extraMetadata) {
            this.paramName = paramName;
            this.paramType = paramType;
            this.extraMetadata = extraMetadata;
        }

        public Map<String, Object> getAsApiErrorMetadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (extraMetadata != null) {
                for (Pair<String, String> pair : extraMetadata) {
                    if (pair != null) {
                        metadata.put(pair.getKey(), pair.getValue());
                    }
                }
            }
            metadata.put("missing_param_name", paramName);
            metadata.put("missing_param_type", paramType);
            return metadata;
        }
    }
}
