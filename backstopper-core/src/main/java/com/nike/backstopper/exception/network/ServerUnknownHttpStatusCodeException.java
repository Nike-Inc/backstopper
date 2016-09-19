package com.nike.backstopper.exception.network;

import java.util.List;
import java.util.Map;

/**
 * Indicates that the downstream server returned a HTTP error status code that is unknown or custom. The exception that
 * represents the server response is contained in {@link #getDetails()} (not to be confused with the
 * {@link #getCause()}, which may not be the same thing, e.g. in the case of the cause wrapping the server response
 * exception). The exact status code, response body, and response headers may or may not be available via
 * {@link #getResponseStatusCode()}, {@link #getRawResponseBody()}, and {@link #getResponseHeaders()}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ServerUnknownHttpStatusCodeException extends NetworkExceptionBase {
    private final Throwable details;
    private final Integer responseStatusCode;
    private final Map<String, List<String>> responseHeaders;
    private final String rawResponseBody;

    public ServerUnknownHttpStatusCodeException(Throwable cause, String connectionType, Throwable details,
                                                Integer responseStatusCode, Map<String, List<String>> responseHeaders,
                                                String rawResponseBody) {
        super(cause, connectionType);
        this.details = details;
        this.responseStatusCode = responseStatusCode;
        this.responseHeaders = responseHeaders;
        this.rawResponseBody = rawResponseBody;
    }

    /**
     * @return The server exception that triggered this instance, or null if this info was not provided.
     */
    public Throwable getDetails() {
        return details;
    }

    /**
     * @return The HTTP status code associated with the weird server response, or null if this info was not provided.
     */
    public Integer getResponseStatusCode() {
        return responseStatusCode;
    }

    /**
     * @return The headers associated with the weird server response, or null if this info was not provided.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @return The raw response body as a string associated with the weird server response, or null if this info was
     * not provided.
     */
    public String getRawResponseBody() {
        return rawResponseBody;
    }
}
