package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.StackTraceLoggingBehavior;
import com.nike.backstopper.exception.WrapperException;
import com.nike.backstopper.exception.network.NetworkExceptionBase;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.internal.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.nike.backstopper.exception.StackTraceLoggingBehavior.FORCE_NO_STACK_TRACE;
import static com.nike.backstopper.exception.StackTraceLoggingBehavior.FORCE_STACK_TRACE;

/**
 * The base class for a main API exception handler. Generally speaking there will be different extension classes for
 * different frameworks that define the {@link T} type and implement the abstract
 * {@link #prepareFrameworkRepresentation(DefaultErrorContractDTO, int, Collection, Throwable, RequestInfoForLogging)}
 * method in a way that makes sense for the framework. Frameworks can often define sane defaults for the
 * list of {@link ApiExceptionHandlerListener}s and {@link ApiExceptionHandlerUtils} that the constructor requires
 * (while still leaving individual projects the capability for overriding the default values), leaving the definition
 * of the {@link ProjectApiErrors} constructor argument the only thing a project needs to do for a complete API
 * exception handler.
 *
 * <p><b>In any case by implementing the abstract method, inserting an implementation of this class into the appropriate
 * place to handle exceptions for a project, calling {@link #maybeHandleException(Throwable, RequestInfoForLogging)}
 * when an exception occurs and using the result to build the response for the caller, this class can serve to
 * completely solve the error handling requirements for a project.</b>
 *
 * <p>If an exception is handled it will be logged along with all the relevant request data and debugging info
 * available.
 *
 * <p>NOTE: An implementation of {@link UnhandledExceptionHandlerBase} should be used as a catch-all for your project
 * in case the implementation of this class fails to catch and handle an exception for any reason.
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
public abstract class ApiExceptionHandlerBase<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ProjectApiErrors projectApiErrors;
    protected final List<ApiExceptionHandlerListener> apiExceptionHandlerListenerList;
    protected final ApiExceptionHandlerUtils utils;

    /**
     * Creates a new instance with the given arguments.
     *
     * @param projectApiErrors The {@link ProjectApiErrors} used for this project - cannot be null.
     * @param apiExceptionHandlerListenerList
     *          The list of {@link ApiExceptionHandlerListener}s that will be used for this project to analyze
     *          exceptions and see if they should be handled (and how they should be handled if so). These will be
     *          executed in list order. This cannot be null (pass in an empty list if you really don't have any
     *          listeners for your project, however this should never be the case in practice - you should always
     *          include {@link com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener}
     *          at the very least).
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     *              {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic. Cannot be null.
     */
    public ApiExceptionHandlerBase(ProjectApiErrors projectApiErrors,
                                   List<ApiExceptionHandlerListener> apiExceptionHandlerListenerList,
                                   ApiExceptionHandlerUtils utils) {
        if (projectApiErrors == null)
            throw new IllegalArgumentException("projectApiErrors cannot be null.");

        if (apiExceptionHandlerListenerList == null)
            throw new IllegalArgumentException("apiExceptionHandlerListenerList cannot be null.");

        if (utils == null)
            throw new IllegalArgumentException("apiExceptionHandlerUtils cannot be null.");

        this.projectApiErrors = projectApiErrors;
        this.apiExceptionHandlerListenerList = apiExceptionHandlerListenerList;
        this.utils = utils;
    }

    /**
     * The default set of exception classes that should be considered "wrapper" exceptions. This is returned by
     * {@link #getWrapperExceptionClassNames()} by default unless you override that method. See
     * {@link #getWrapperExceptionClassNames()} and {@link #unwrapAndFindCoreException(Throwable)} for more details.
     */
    public final Set<String> DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES = new HashSet<>(Arrays.asList(
            WrapperException.class.getName(),
            ExecutionException.class.getName(),
            "java.util.concurrent.CompletionException"
    ));

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
     *          {@link #maybeHandleException(Throwable, RequestInfoForLogging)}, or null if you don't have any extra
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
     * @param ex The exception that this class may or may not want to handle.
     * @param request The incoming request.
     * @return The object that the framework or project will translate into the error response for the client, or null
     *          if this class did not want to handle the exception (and in that case an implementation of
     *          {@link UnhandledExceptionHandlerBase} should be used as a failsafe to handle it).
     *
     * @throws UnexpectedMajorExceptionHandlingError This will be thrown if an unexpected error occurred while trying to
     *          execute this method. This should never be thrown under normal circumstances and likely indicates a bug
     *          in this class that needs to be fixed. Callers should still catch this exception and do something
     *          appropriate if it is thrown (usually passing the original exception on to the project's implementation
     *          of {@link UnhandledExceptionHandlerBase}).
     */
    public ErrorResponseInfo<T> maybeHandleException(Throwable ex, RequestInfoForLogging request)
        throws UnexpectedMajorExceptionHandlingError
    {
        try {
            ApiExceptionHandlerListenerResult result = shouldHandleApiException(ex);

            if (result.shouldHandleResponse)
                return doHandleApiException(result.errors, result.extraDetailsForLogging, result.extraResponseHeaders,
                                            ex, request);
        }
        catch(Exception ohNoException) {
            throw new UnexpectedMajorExceptionHandlingError(
                "Unexpected major error in " + this.getClass().getName() + ". We had an inner exception while trying "
                + "to handle the original controller exception. This needs to be fixed ASAP. "
                + "major_error_in_api_exception_handler=true",
                ohNoException
            );
        }

        // Any other exceptions should be handled by an UnhandledExceptionHandlerBase implementation
        return null;
    }

    /**
     * @return An {@link ApiExceptionHandlerListenerResult} indicating whether we should handle the given exception.
     *          If {@link ApiExceptionHandlerListenerResult#shouldHandleResponse} is true then
     *          {@link ApiExceptionHandlerListenerResult#errors} and
     *          {@link ApiExceptionHandlerListenerResult#extraDetailsForLogging} must be filled in appropriately and
     *          ready for passing in to
     *          {@link #doHandleApiException(SortedApiErrorSet, List, List, Throwable, RequestInfoForLogging)}. If it is
     *          false then the given exception will be ignored by this class (and should therefore ultimately be handled
     *          by this project's implementation of {@link UnhandledExceptionHandlerBase}).
     */
    protected ApiExceptionHandlerListenerResult shouldHandleApiException(Throwable ex) {
        // The original exception might be a "wrapper" exception. If so, unwrap it so we can send the core exception
        //      through our list of listeners.
        Throwable coreEx = unwrapAndFindCoreException(ex);

        // Run through each listener looking for one that wants to handle the core exception.
        for (ApiExceptionHandlerListener listener : apiExceptionHandlerListenerList) {
            ApiExceptionHandlerListenerResult result = listener.shouldHandleException(coreEx);
            if (result.shouldHandleResponse)
                return result;
        }

        // We didn't have any handler that wanted to deal with this exception, so return an "ignore it" response.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }

    /**
     * "Unwraps" the given exception by digging through the {@link Throwable#getCause()} chain until a non-wrapper
     * exception type is found. Uses {@link #getWrapperExceptionClassNames()} as the set of exception classes that are
     * considered wrappers.
     *
     * @param error The exception that may (or may not) need to be "unwrapped".
     * @return The root/core cause exception that is not a wrapper exception - the passed-in exception will be returned
     *          as-is if it is not a wrapper exception or if it has no cause.
     */
    protected Throwable unwrapAndFindCoreException(Throwable error) {
        if (error == null || error.getCause() == null || error.getCause() == error)
            return error;

        // At this point there must be a non-null cause, and it is not a reference to itself. See if it's a wrapper.
        if (getWrapperExceptionClassNames().contains(error.getClass().getName())) {
            // This is a wrapper. Extract the cause.
            error = error.getCause();
            // Recursively unwrap until we get something that is not unwrappable
            error = unwrapAndFindCoreException(error);
        }

        return error;
    }

    /**
     * @return The set of exception classes that should be considered "wrapper" exceptions, used by
     *          {@link #unwrapAndFindCoreException(Throwable)}. Returns {@link #DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES}
     *          by default - override this if you have your own exception classes you want considered wrapper
     *          exceptions.
     */
    protected Set<String> getWrapperExceptionClassNames() {
        return DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES;
    }

    /**
     * Helper method for {@link #maybeHandleException(Throwable, RequestInfoForLogging)} that handles the nitty gritty
     * of logging the appropriate request info, converting the errors into an {@link DefaultErrorContractDTO}, and using
     * {@link #prepareFrameworkRepresentation(DefaultErrorContractDTO, int, Collection, Throwable,
     * RequestInfoForLogging)} to generate the appropriate response for the client.
     *  @param clientErrors The ApiErrors that the originalException converted into.
     * @param extraDetailsForLogging Any extra details that should be logged along with the usual request/error info.
     * @param extraResponseHeaders Any extra response headers that should be sent to the caller in the HTTP response.
     * @param originalException The original exception that we are handling.
     * @param request The incoming request.
     */
    protected ErrorResponseInfo<T> doHandleApiException(
      SortedApiErrorSet clientErrors, List<Pair<String, String>> extraDetailsForLogging,
      List<Pair<String, List<String>>> extraResponseHeaders, Throwable originalException,
      RequestInfoForLogging request
    ) {
        Throwable coreException = unwrapAndFindCoreException(originalException);

        // Add connection type to our extra logging data if appropriate. This particular log message is here so it can
        //      be done in one spot rather than trying to track down all the different places we're handling
        //      NetworkExceptionBase subclasses (and possibly missing some by accident).
        if (coreException instanceof NetworkExceptionBase neb) {
            extraDetailsForLogging.add(Pair.of("connection_type", neb.getConnectionType()));
        }

        // We may need to drop some of our client errors if we have a mix of http status codes (see javadocs on
        //      ProjectApiError's determineHighestPriorityHttpStatusCode and getSublistContainingOnlyHttpStatusCode
        //      methods).
        Integer highestPriorityStatusCode = projectApiErrors.determineHighestPriorityHttpStatusCode(clientErrors);
        Collection<ApiError> filteredClientErrors =
            projectApiErrors.getSublistContainingOnlyHttpStatusCode(clientErrors, highestPriorityStatusCode);

        // Bulletproof against somehow getting a completely empty collection of client errors. This should never happen
        //      but if it does we want a reasonable response.
        if (filteredClientErrors == null || filteredClientErrors.isEmpty()) {
            ApiError genericServiceError = projectApiErrors.getGenericServiceError();
            UUID trackingUuid = UUID.randomUUID();
            String trackingLogKey = "bad_handler_logic_tracking_uuid";
            extraDetailsForLogging.add(Pair.of(trackingLogKey, trackingUuid.toString()));
            logger.error(
                "Found a situation where we ended up with 0 ApiErrors to return to the client. This should not happen "
                + "and likely indicates a logic error in ApiExceptionHandlerBase, or a ProjectApiErrors that isn't "
                + "setup properly. Defaulting to {} for now, but this should be "
                + "investigated and fixed. Search for {}={} in the logs to find the log message that contains the "
                + "details of the request along with the full stack trace of the original exception. "
                + "unfiltered_api_errors={}",
                genericServiceError.getName(), trackingLogKey, trackingUuid, utils.concatenateErrorCollection(clientErrors)
            );
            filteredClientErrors = Collections.singletonList(genericServiceError);
            highestPriorityStatusCode = genericServiceError.getHttpStatusCode();
        }

        // Log all the relevant error/debugging info.
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("ApiExceptionHandlerBase handled exception occurred: ");
        String errorId = utils.buildErrorMessageForLogs(
            logMessage, request, filteredClientErrors, highestPriorityStatusCode, coreException, extraDetailsForLogging
        );

        // Don't log the stack trace on 4xx validation exceptions, but do log it on anything else.
        if (shouldLogStackTrace(
            highestPriorityStatusCode, filteredClientErrors, originalException, coreException, request
        )) {
            logger.error(logMessage.toString(), originalException);
        }
        else {
            logger.warn(logMessage.toString());
        }

        // Generate our internal default representation of the error contract (the DefaultErrorContractDTO), and
        //      translate it into the representation required by the framework.
        DefaultErrorContractDTO errorContractDTO = new DefaultErrorContractDTO(errorId, filteredClientErrors);
        T frameworkRepresentation = prepareFrameworkRepresentation(
            errorContractDTO, highestPriorityStatusCode, filteredClientErrors, originalException, request
        );

        // Setup the final additional response headers that should be sent back to the caller.
        Map<String, List<String>> finalHeadersForResponse = new HashMap<>();
        // Start with any extra headers that came into this method.
        if (extraResponseHeaders != null) {
            for (Pair<String, List<String>> addMe : extraResponseHeaders) {
                finalHeadersForResponse.put(addMe.getLeft(), addMe.getRight());
            }
        }
        // Then add any from the extraHeadersForResponse() method from this class.
        Map<String, List<String>> evenMoreExtraHeadersForResponse = extraHeadersForResponse(
          frameworkRepresentation, errorContractDTO, highestPriorityStatusCode, filteredClientErrors,
          originalException, request
        );
        if (evenMoreExtraHeadersForResponse != null)
            finalHeadersForResponse.putAll(evenMoreExtraHeadersForResponse);
        // Always add the error_uid header that matches the errorId that was generated.
        finalHeadersForResponse.put("error_uid", Collections.singletonList(errorId));

        // Finally, return the ErrorResponseInfo with the status code, framework response, and headers for the response.
        return new ErrorResponseInfo<>(highestPriorityStatusCode, frameworkRepresentation, finalHeadersForResponse);
    }

    /**
     * @param statusCode The HTTP status code associated with this error.
     * @param filteredClientErrors The filtered collection of {@link ApiError}s associated with this error.
     * @param originalException The original error.
     * @param coreException The core exception after the original was passed through
     *                      {@link #unwrapAndFindCoreException(Throwable)}. This may be the same object as
     *                      originalException.
     * @param request The request info.
     *
     * @return true if the given originalException should be logged at error log level with a stack trace, false if it
     *          should be logged at warn log level with only the class type and message. By default this method
     *          will return false for 4xx HTTP status code errors, true for everything else. This method honors
     *          the case where coreException is an {@link ApiException} and {@link
     *          ApiException#getStackTraceLoggingBehavior()} asks to force the stack trace logging on or off.
     *          Override this method if you need different behavior.
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean shouldLogStackTrace(
        int statusCode,
        @SuppressWarnings("unused") Collection<ApiError> filteredClientErrors,
        @SuppressWarnings("unused") Throwable originalException,
        Throwable coreException,
        @SuppressWarnings("unused") RequestInfoForLogging request
    ) {
        if (coreException instanceof ApiException) {
            // See if this ApiException is explicitly requesting stack trace logging to be forced on or off.
            StackTraceLoggingBehavior stackTraceLoggingBehavior =
                ((ApiException)coreException).getStackTraceLoggingBehavior();

            if (stackTraceLoggingBehavior == FORCE_STACK_TRACE) {
                return true;
            }
            else if (stackTraceLoggingBehavior == FORCE_NO_STACK_TRACE) {
                return false;
            }

            // If we reach here then stackTraceLoggingBehavior is null or DEFER_TO_DEFAULT_BEHAVIOR.
            //      In either case, we want the default logic to be used to determine whether the stack trace is logged.
        }

        // By default, 4xx should *not* log stack trace. Everything else should.
        return statusCode < 400 || statusCode >= 500;
    }
}
