package com.nike.backstopper.handler;

import java.util.List;
import java.util.Map;

/**
 * Interface representing request information that is useful for logging. There are adapters for the request types of
 * various frameworks (e.g. Servlet, Netty, etc) so that you can easily create an instance of this interface for
 * whatever request object you happen to be using. If no adapter currently exists then you can create one.
 *
 * @author Nic Munroe
 */
public interface RequestInfoForLogging {

    /**
     * @return The URI associated with the request. This should not include the scheme, host, port, or query string -
     *          just the request URI. e.g. If the full request was
     *          {@code http://some.location:8080/path/to/resource.html?foo=bar}, then this method would return
     *          {@code /path/to/resource.html}.
     */
    String getRequestUri();

    /**
     * @return The HTTP method associated with the request (e.g. GET, POST, PUT, PATCH, etc).
     */
    String getRequestHttpMethod();

    /**
     * @return The query string associated with the request, or null if there is no query string.
     */
    String getQueryString();

    /**
     * @return The map of headers associated with the request, or an empty map if there are no headers. Should never
     *          return null.
     */
    Map<String, List<String>> getHeadersMap();

    /**
     * @return The header value associated with the given header name for this request, or null if no such header was
     *          available. If the header is a multi-value header, then this method should just return the first item
     *          in the value list.
     */
    String getHeader(String headerName);

    /**
     * @return The list of header values associated with the given header name for this request, or null if no such
     *          header was available.
     */
    List<String> getHeaders(String headerName);

    /**
     * @return The attribute object associated with the given key for this request, or null if no such attribute was
     *          available. This method may always return null if the request type this interface is wrapping doesn't
     *          support request attributes.
     */
    Object getAttribute(String key);

    /**
     * @return The body associated with the request, or an empty string if there was no body. Should never return null,
     *          and implementations should take care to catch all {@link Throwable}s and convert to
     *          {@link GetBodyException} rather than allow arbitrary runtime exceptions to bubble out of this method.
     *          <p>WARNING: THIS CALL CAN BE EXPENSIVE DEPENDING ON THE REQUEST PAYLOAD AND/OR FRAMEWORK - CALL THIS
     *          ONLY WHEN ABSOLUTELY NECESSARY.
     *          <p>ALSO NOTE: In many frameworks if the body has already been read once then it cannot be read again,
     *          so this method may or may not work depending on when it is called and what framework you're in -
     *          it may throw a {@link GetBodyException}, or it may simply return a blank string (again, this depends
     *          on how your framework works).
     */
    String getBody() throws GetBodyException;

    /**
     * Exception class to indicate any possible error that can happen while reading the HTTP body.
     */
    class GetBodyException extends Exception {
        public GetBodyException(String message, Throwable ex) {
            super(message, ex);
        }
    }
}
