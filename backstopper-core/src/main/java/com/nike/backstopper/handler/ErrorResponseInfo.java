package com.nike.backstopper.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object that contains data related to the response for an error handled by {@link ApiExceptionHandlerBase} or
 * {@link UnhandledExceptionHandlerBase}. Frameworks should use this information to generate the final response
 * to send back to the caller.
 *
 * @author Nic Munroe
 */
public class ErrorResponseInfo<T> {

    /**
     * The HTTP status code that should be returned in the response to the caller. This is not automatically registered
     * on the framework's response - you should set this yourself on the response after you call an error handler.
     */
    public final int httpStatusCode;
    /**
     * The framework representation object. Might represent response body content, or the whole response - it's up to
     * each framework/implementation to know what to do with this.
     */
    public final T frameworkRepresentationObj;
    /**
     * Extra headers that were generated during error handling (e.g. error_uid) that should be added as headers to the
     * response sent to the user. These are not automatically registered on the framework's response - you should set
     * these yourself on the response after you call an error handler. This will never be null - it will be an empty map
     * if there are no headers to add.
     */
    public final Map<String, List<String>> headersToAddToResponse = new HashMap<>();

    public ErrorResponseInfo(int httpStatusCode, T frameworkRepresentationObj,
                             Map<String, List<String>> headersToAddToResponse) {

        this.httpStatusCode = httpStatusCode;
        this.frameworkRepresentationObj = frameworkRepresentationObj;
        if (headersToAddToResponse != null)
            this.headersToAddToResponse.putAll(headersToAddToResponse);
    }
}
