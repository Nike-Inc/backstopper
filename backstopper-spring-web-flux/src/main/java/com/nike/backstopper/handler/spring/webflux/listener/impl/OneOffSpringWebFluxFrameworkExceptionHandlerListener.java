package com.nike.backstopper.handler.spring.webflux.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.spring.listener.impl.OneOffSpringCommonFrameworkExceptionHandlerListener;
import com.nike.internal.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.server.MediaTypeNotSupportedStatusException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * An extension and concrete implementation of {@link OneOffSpringCommonFrameworkExceptionHandlerListener} that
 * knows how to handle Spring WebFlux specific exceptions.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class OneOffSpringWebFluxFrameworkExceptionHandlerListener
    extends OneOffSpringCommonFrameworkExceptionHandlerListener {

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     * ApiError}s. Cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     * {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    @Inject
    public OneOffSpringWebFluxFrameworkExceptionHandlerListener(ProjectApiErrors projectApiErrors,
                                                                ApiExceptionHandlerUtils utils) {
        super(projectApiErrors, utils);
    }

    @Override
    protected @NotNull ApiExceptionHandlerListenerResult handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
        @NotNull Throwable ex
    ) {
        if (ex instanceof ResponseStatusException) {
            return handleResponseStatusException((ResponseStatusException)ex);
        }

        // This exception is not handled here.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    protected @NotNull ApiExceptionHandlerListenerResult handleResponseStatusException(
        @NotNull ResponseStatusException ex
    ) {
        // ResponseStatusException is technically in spring-web, so it could be handled in backstopper-spring-web's
        //      OneOffSpringCommonFrameworkExceptionHandlerListener, except that it's also spring 5 so we'd have to
        //      have yet another module for backstopper-spring-web5. Or we'd have to do a bunch of obnoxious reflection.
        //      Since Spring WebFlux seems to be the only place ResponseStatusException is used, we'll just shove this
        //      logic here for now. It can be moved later if needed.
        int statusCode = ex.getStatus().value();
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
        addExtraDetailsForLoggingForResponseStatusException(ex, extraDetailsForLogging);

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
                return handleTypeMismatchException(tmeCause, extraDetailsForLogging, false);
            }
        }
        else if (exCause instanceof DecodingException && statusCode == 400) {
            return handleError(projectApiErrors.getMalformedRequestApiError(), extraDetailsForLogging);
        }

        // Exception cause didn't help. Try parsing the reason message.
        String exReason = (ex.getReason() == null) ? "" : ex.getReason();
        String[] exReasonWords = exReason.split(" ");

        RequiredParamData missingRequiredParam = parseExReasonForMissingRequiredParam(exReasonWords, exReason);
        if (missingRequiredParam != null && statusCode == 400) {
            return handleError(
                new ApiErrorWithMetadata(
                    projectApiErrors.getMalformedRequestApiError(),
                    Pair.of("missing_param_name", missingRequiredParam.paramName),
                    Pair.of("missing_param_type", missingRequiredParam.paramType)
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

    protected void addExtraDetailsForLoggingForResponseStatusException(
        @NotNull ResponseStatusException ex,
        @NotNull List<Pair<String, String>> extraDetailsForLogging
    ) {
        if (ex instanceof MediaTypeNotSupportedStatusException) {
            MediaTypeNotSupportedStatusException detailsEx = (MediaTypeNotSupportedStatusException)ex;
            extraDetailsForLogging.add(
                Pair.of("supported_media_types", concatenateCollectionToString(detailsEx.getSupportedMediaTypes()))
            );
        }

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
        switch (statusCode) {
            case 400:
                return projectApiErrors.getGenericBadRequestApiError();
            case 401:
                return projectApiErrors.getUnauthorizedApiError();
            case 403:
                return projectApiErrors.getForbiddenApiError();
            case 404:
                return projectApiErrors.getNotFoundApiError();
            case 405:
                return projectApiErrors.getMethodNotAllowedApiError();
            case 406:
                return projectApiErrors.getNoAcceptableRepresentationApiError();
            case 415:
                return projectApiErrors.getUnsupportedMediaTypeApiError();
            case 429:
                return projectApiErrors.getTooManyRequestsApiError();
            case 500:
                return projectApiErrors.getGenericServiceError();
            case 503:
                return projectApiErrors.getTemporaryServiceProblemApiError();
        }

        // If we reach here then it wasn't a status code where we have a common ApiError in ProjectApiErrors.
        //      Generate a generic ApiError to cover it.
        return generateGenericApiErrorForResponseStatusCode(statusCode);
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
        @NotNull String[] exReasonWords, @NotNull String exReason
    ) {
        if (exReasonWords.length != 7) {
            return null;
        }

        if ("Required".equals(exReasonWords[0])
            && "parameter".equals(exReasonWords[2])
            && exReason.endsWith("is not present")
        ) {
            return new RequiredParamData(exReasonWords[3].replace("'", ""), exReasonWords[1]);
        }

        return null;
    }

    protected static class RequiredParamData {
        public final String paramName;
        public final String paramType;

        public RequiredParamData(String paramName, String paramType) {
            this.paramName = paramName;
            this.paramType = paramType;
        }
    }
}
