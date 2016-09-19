package com.nike.backstopper.apierror;

/**
 * Contains some common constants related to API Errors. This is not exhaustive.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("unused")
public class ApiErrorConstants {

    /**
     * {@code 400 Bad Request}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;
    /**
     * {@code 401 Unauthorized}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1 Authentication</a>
     */
    public static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;
    /**
     * {@code 403 Forbidden}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_FORBIDDEN = 403;
    /**
     * {@code 404 Not Found}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;
    /**
     * {@code 405 Method Not Allowed}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_METHOD_NOT_ALLOWED = 405;
    /**
     * {@code 406 Not Acceptable}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_NOT_ACCEPTABLE = 406;
    /**
     * {@code 409 Conflict}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_CONFLICT = 409;
    /**
     * {@code 413 Payload Too Large}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_PAYLOAD_TOO_LARGE = 413;
    /**
     * {@code 415 Unsupported Media Type}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE = 415;
    /**
     * {@code 429 Too Many Requests}.
     *
     * @see <a href="http://tools.ietf.org/html/rfc6585#section-4">Additional HTTP Status Codes</a>
     */
    public static final int HTTP_STATUS_CODE_TOO_MANY_REQUESTS = 429;

    /**
     * {@code 500 Internal Server Error}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR = 500;
    /**
     * {@code 503 Service Unavailable}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1 Semantics and Content</a>
     */
    public static final int HTTP_STATUS_CODE_SERVICE_UNAVAILABLE = 503;

}
