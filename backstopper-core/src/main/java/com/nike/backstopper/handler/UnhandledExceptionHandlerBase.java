package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.internal.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base exception handler intended to be used as a catch-all or last-resort exception handler for when
 * the main project's {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} failed to handle an
 * exception (either due to unexpected errors or because the exception is unknown and went unhandled by all of
 * the normal exception listeners).
 *
 * <p>This is similar to {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} in that
 * {@link #handleException(Throwable, RequestInfoForLogging)} returns an object for fulfilling the project's
 * error contract requirements in a format appropriate for the current framework/project. It will log as much info as
 * possible, and then return an error contract that consists only of {@link ProjectApiErrors#getGenericServiceError()}.
 * It differs in that this class is designed to never fail and always return a response.
 *
 * @param <T> The type of object that the framework requires to be returned as the response (or the type that you simply
 *           want to convert the error contract to for whatever reason). For some frameworks this may just be the raw
 *           {@link DefaultErrorContractDTO} that will be serialized to the output format directly (e.g. JSON) to
 *           satisfy the error contract. Other frameworks may require some kind of wrapper object or other
 *           representation. This can be the final response object, or some kind of intermediate object that is further
 *           transformed outside the bounds of this class - that decision is up to the implementor. The recommended
 *           pattern is to have this type be an object representing the response body only, and the framework transforms
 *           the resulting {@link ErrorResponseInfo} to the final response for the caller outside the bounds of this
 *           class.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class UnhandledExceptionHandlerBase<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ProjectApiErrors projectApiErrors;
    protected final ApiExceptionHandlerUtils utils;

    /**
     * Creates a new instance with the given arguments.
     *
     * @param projectApiErrors The {@link ProjectApiErrors} used for this project - cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     *              {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    public UnhandledExceptionHandlerBase(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
        if (projectApiErrors == null)
            throw new IllegalArgumentException("projectApiErrors cannot be null.");

        if (utils == null)
            throw new IllegalArgumentException("apiExceptionHandlerUtils cannot be null.");

        this.projectApiErrors = projectApiErrors;
        this.utils = utils;
    }

    /**
     * @param errorContractDTO The default internal representation model DTO of the error contract that should be
     *                         returned to the client.
     * @param httpStatusCode The calculated HTTP status code that the {@code errorContractDTO} represents and should be
     *                       returned in the response to the caller.
     * @param rawFilteredApiErrors
     *          The collection of raw {@link ApiError}s that were used to create {@code errorContractDTO} - most of the
     *          time this can be ignored, however it is supplied here in case there's some relevant info you need for
     *          generating the framework response that is not contained in {@code errorContractDTO}.
     * @param originalException The original exception that was handled - most of the time this can be ignored, however
     *                          it is supplied here in case there's some relevant info you need for generating the
     *                          framework response that is not contained in {@code errorContractDTO}.
     * @param request The request info that was passed in and used for logging - most of the time this can be ignored,
     *                however it is supplied here in case there's some relevant info you need for generating the
     *                framework response that is not contained in {@code errorContractDTO}.
     * @return The object required by the framework to represent the error contract to send to the caller. This may
     *          simply be the given {@link DefaultErrorContractDTO} if the framework is able to use the object directly
     *          to convert to the necessary serialized representation (e.g. JSON via Jackson), or it might be a wrapper
     *          object or some other representation required by the framework as long as it will ultimately appear to
     *          the client as the desired final error contract.
     */
    protected abstract T prepareFrameworkRepresentation(
        DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException, RequestInfoForLogging request);

    /**
     * This will be called as a last resort if {@link #handleException(Throwable, RequestInfoForLogging)} blew up
     * with a secondary unexpected exception while it was trying to handle the original exception.
     *
     * <p><b>IMPORTANT NOTE: THIS MUST ALWAYS RETURN A NON-NULL OBJECT AND MUST NEVER THROW AN EXCEPTION UNDER ANY
     * CIRCUMSTANCES!</b>. If this blows up, then there is no more protection - you may get a raw framework error
     * response sent back to the caller with information leakage, stack traces, etc. It's highly recommended that this
     * method's implementation be as simple and bulletproof as possible, maybe even using a predefined return object
     * so that there's no possibility of failure (maybe with the {@code errorUid} included in the response payload
     * using string replacement or something similarly bulletproof). It's also recommended that the return object
     * represent a basic HTTP status code 500 service error.
     *
     * @param ex The original exception that was passed to {@link #handleException(Throwable, RequestInfoForLogging)}.
     * @param request The original request that was passed to
     *                {@link #handleException(Throwable, RequestInfoForLogging)}.
     * @param errorUid The error ID that the log messages regarding this situation were tagged with, and is attached
     *                 to the given {@code headersForResponseWithErrorUid} under the header key {@code error_uid}.
     * @param headersForResponseWithErrorUid
     *          A map of headers that already contains an {@code error_uid} header key with the value of the
     *          {@code errorUid} argument. You can add to this map if you want, and then use it to populate the returned
     *          {@link ErrorResponseInfo#headersToAddToResponse}.
     * @return The {@link ErrorResponseInfo} that should be sent back to the caller - see the overview javadocs for
     *          this method for recommendations and warnings on how this should be built.
     */
    protected abstract ErrorResponseInfo<T> generateLastDitchFallbackErrorResponseInfo(
        Throwable ex, RequestInfoForLogging request, String errorUid,
        Map<String, List<String>> headersForResponseWithErrorUid);

    /**
     * @param frameworkRepresentation The framework representation generated by
     *    {@link #prepareFrameworkRepresentation(DefaultErrorContractDTO, int, Collection, Throwable,
     *    RequestInfoForLogging)}.
     * @param errorContractDTO The default internal representation model DTO of the error contract that was generated
     *                         based on {@code rawFilteredApiErrors}.
     * @param httpStatusCode The calculated HTTP status code that the {@code errorContractDTO} represents and should be
     *                       returned with the error contract.
     * @param rawFilteredApiErrors The collection of raw {@link ApiError}s that were used to create
     *                              {@code errorContractDTO}.
     * @param originalException The original exception that was handled.
     * @param request The request info that was passed in and used for logging.
     * @return A map of the desired extra headers that should be included in the
     *          {@link ErrorResponseInfo#headersToAddToResponse} map returned by
     *          {@link #handleException(Throwable, RequestInfoForLogging)}, or null if you don't have any extra
     *          headers you want added. The error_uid header will automatically be included in
     *          {@link ErrorResponseInfo#headersToAddToResponse} so you should not attempt to add that here.
     */
    protected Map<String, List<String>> extraHeadersForResponse(
        T frameworkRepresentation, DefaultErrorContractDTO errorContractDTO, int httpStatusCode,
        Collection<ApiError> rawFilteredApiErrors, Throwable originalException, RequestInfoForLogging request
    ) {
        return null;
    }

    /**
     * @param ex The exception that this method <b>must</b> handle.
     * @param request The incoming request.
     * @return The object that the framework or project will translate into the error contract for the client.
     *          This method will *never* return null, and {@link ProjectApiErrors#getGenericServiceError()} will
     *          always be used as the {@link ApiError} representing the given exception.
     */
    public ErrorResponseInfo<T> handleException(Throwable ex, RequestInfoForLogging request) {
        String errorUid = null;
        try {
            List<ApiError> errorsToUse = Collections.singletonList(projectApiErrors.getGenericServiceError());
            int httpStatusCodeToUse = projectApiErrors.determineHighestPriorityHttpStatusCode(errorsToUse);

            // At this point we handle *all* exceptions. Start by first logging the exception with a error ID and other
            //      relevant request data.
            StringBuilder baseLogMessageBuilder = new StringBuilder();
            baseLogMessageBuilder.append("Caught unhandled exception: ");

            List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
            extraDetailsForLogging.add(Pair.of("unhandled_error", "true"));

            if (logRequestBodyOnUnhandledExceptions(ex, request)) {
                String body;
                try {
                    body = request.getBody();
                } catch (RequestInfoForLogging.GetBodyException e) {
                    logger.warn("Failed to retrieve request_body while handling exception ex={}", ex, e);
                    body = "[ERROR_EXTRACING_BODY]";
                }

                extraDetailsForLogging.add(Pair.of("request_body", body));
            }

            errorUid = utils.buildErrorMessageForLogs(
                baseLogMessageBuilder, request, errorsToUse, httpStatusCodeToUse, ex, extraDetailsForLogging
            );
            // Always log at error log level and include the stack trace for unhandled exceptions like this.
            logger.error(baseLogMessageBuilder.toString(), ex);

            // Generate our internal default representation of the error contract (the DefaultErrorContractDTO), and
            // translate it into the representation required by the framework.
            DefaultErrorContractDTO errorContractDTO = new DefaultErrorContractDTO(errorUid, errorsToUse);
            T frameworkRepresentation = prepareFrameworkRepresentation(
                errorContractDTO, httpStatusCodeToUse, errorsToUse, ex, request
            );

            // Get any extra headers desired for the response.
            Map<String, List<String>> extraHeadersForResponse = extraHeadersForResponse(
                frameworkRepresentation, errorContractDTO, httpStatusCodeToUse, errorsToUse, ex, request
            );
            Map<String, List<String>> finalHeadersForResponse = new HashMap<>();
            if (extraHeadersForResponse != null)
                finalHeadersForResponse.putAll(extraHeadersForResponse);
            // Always add the error_uid header that matches the errorUid that was generated.
            finalHeadersForResponse.put("error_uid", Collections.singletonList(errorUid));

            // Finally, return the ErrorResponseInfo with the status code, framework response, and headers for the response.
            return new ErrorResponseInfo<>(httpStatusCodeToUse, frameworkRepresentation, finalHeadersForResponse);
        }
        catch(Throwable ohWowThisIsBad) {
            // Reuse the errorUid generated above if possible. Otherwise create a new one.
            errorUid = (errorUid == null) ? UUID.randomUUID().toString() : errorUid;
            logger.error("IMPORTANT ERROR (error_uid={}): {} failed to properly handle an exception and blew up with "
                         + "a secondary exception. Original exception passed to the handleException(...) method:",
                         errorUid, this.getClass().getName(), ex);
            logger.error("IMPORTANT ERROR (error_uid={}): {} failed to properly handle an exception and blew up with "
                         + "a secondary exception. Secondary exception thrown by the handleException(...) method (this "
                         + "is a bug that needs to be fixed!):",
                         errorUid, this.getClass().getName(), ohWowThisIsBad);

            Map<String, List<String>> finalHeadersForResponse = new HashMap<>();
            finalHeadersForResponse.put("error_uid", Collections.singletonList(errorUid));
            return generateLastDitchFallbackErrorResponseInfo(ex, request, errorUid, finalHeadersForResponse);
        }
    }

    /**
     * @param ex The exception that this class is handling.
     * @param request The incoming request.
     *
     * @return true if {@link #handleException(Throwable, RequestInfoForLogging)} should attempt to log the request
     *          body with the message it spits out to the logs, false if it should not include the request body.
     *          Note that in many frameworks if the body has already been read once then it cannot be read again,
     *          so you may have more work to do than just setting this method to return true.
     */
    @SuppressWarnings("unused")
    protected boolean logRequestBodyOnUnhandledExceptions(Throwable ex, RequestInfoForLogging request) {
        return false;
    }
}
