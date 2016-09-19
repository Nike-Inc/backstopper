package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.internal.util.Pair;
import com.nike.internal.util.StringUtils;

import org.slf4j.MDC;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Set of reusable utility methods used by the API exception handling chain
 * ({@link com.nike.backstopper.handler.ApiExceptionHandlerBase},
 * {@link com.nike.backstopper.handler.UnhandledExceptionHandlerBase}, and the various
 * {@link com.nike.backstopper.handler.listener.ApiExceptionHandlerListener} implementations).
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class ApiExceptionHandlerUtils {

    /**
     * The default implementation of {@link ApiExceptionHandlerUtils} that masks {@link #DEFAULT_MASKED_HEADER_KEYS}
     * and uses {@link #DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY} when extracting trace ID for the logs. You can override
     * this class and its methods if you need alternate behavior.
     */
    public static final ApiExceptionHandlerUtils DEFAULT_IMPL = new ApiExceptionHandlerUtils();

    /**
     * Constant for the Authorization header key.
     */
    public static final String AUTH_HEADER_KEY = "Authorization";
    /**
     * The default set of header keys that will be masked (hidden) when headers are output to the logs.
     */
    public static final Set<String> DEFAULT_MASKED_HEADER_KEYS = Collections.singleton(AUTH_HEADER_KEY);
    /**
     * The default header key that will be used when trying to determine the current distributed trace ID.
     * This particular key is from the B3 system used by Zipkin and Wingtips (and others).
     */
    public static final String DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY = "X-B3-TraceId";
    /**
     * The {@link MDC} key for trace ID - used by <a href="https://github.com/Nike-Inc/wingtips">Wingtips</a>
     * (for example) to store the current distributed tracing span's trace ID. We can use this in some cases to extract
     * the current trace ID without pulling in any other library dependencies.
     */
    protected static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * Set to true if you want to mask any of the {@link #sensitiveHeaderKeysForMasking} headers, false if all headers
     * should be output as-is.
     */
    protected final boolean maskSensitiveHeaders;
    /**
     * Header keys for sensitive headers that should be masked when logging.
     */
    protected final Set<String> sensitiveHeaderKeysForMasking;
    /**
     * The header key for the distributed trace ID header.
     */
    protected final String distributedTraceIdHeaderKey;

    /**
     * Default constructor that causes this instance to mask {@link #DEFAULT_MASKED_HEADER_KEYS} headers and use
     * {@link #DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY} when extracting trace ID for the logs.
     */
    public ApiExceptionHandlerUtils() {
        this(true, DEFAULT_MASKED_HEADER_KEYS, DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY);
    }

    /**
     * Kitchen sink constructor that lets you set header masking and distributed trace extraction behavior via the
     * constructor arguments.
     *
     * @param maskSensitiveHeaders Set to true if you want {@code sensitiveHeaderKeysForMasking} to be masked when
     *                             outputting headers to the logs.
     * @param sensitiveHeaderKeysForMasking The headers that should be masked if {@code maskSensitiveHeaders} is
     *                                      set to true. {@code maskSensitiveHeaders} will be automatically
     *                                      set to false (disabled) if this argument is null or empty.
     * @param distributedTraceIdHeaderKey The header key that should be used when attempting to extract the distributed
     *                                    trace ID for the request. This can safely be null if you don't expect
     *                                    distributed tracing info to be available in the request headers or attributes.
     */
    public ApiExceptionHandlerUtils(boolean maskSensitiveHeaders, Set<String> sensitiveHeaderKeysForMasking,
                                    String distributedTraceIdHeaderKey) {
        if (sensitiveHeaderKeysForMasking == null)
            sensitiveHeaderKeysForMasking = Collections.emptySet();

        if (sensitiveHeaderKeysForMasking.isEmpty())
            maskSensitiveHeaders = false;

        this.maskSensitiveHeaders = maskSensitiveHeaders;
        this.sensitiveHeaderKeysForMasking = sensitiveHeaderKeysForMasking;
        this.distributedTraceIdHeaderKey = distributedTraceIdHeaderKey;
    }

    /**
     * Adds the given exception's {@link Exception#getMessage()} to the given extraDetailsForLogging with the key of
     * "exception_message" and with the exception's message pruned of quotes via {@link #quotesToApostrophes(String)}.
     */
    public void addBaseExceptionMessageToExtraDetailsForLogging(Throwable ex,
                                                                List<Pair<String, String>> extraDetailsForLogging) {
        extraDetailsForLogging.add(Pair.of("exception_message", quotesToApostrophes(ex.getMessage())));
    }

    /**
     *  @return The given raw string after it has had all its quotes (") replaced with apostrophes ('), or null if the
     *          given raw string is null.
     */
    public String quotesToApostrophes(String raw) {
        if (raw == null)
            return null;

        return raw.replace('\"', '\'');
    }

    /**
     * @return The distributed trace ID if available in the request or the SLF4J {@link MDC}, or null if it cannot be
     *          found. Will also return null if the distributed trace ID exists but its trimmed length is 0
     *          (i.e. the distributed trace ID must be non-empty and contain something besides whitespace for it to be
     *          used). If you are using a distributed tracing system that uses different keys or where the trace ID is
     *          otherwise unobtainable using the rules defined here, then you can override this method and provide
     *          whatever rules you want.
     */
    public String extractDistributedTraceId(RequestInfoForLogging request) {
        String traceIdToUse = null;

        if (distributedTraceIdHeaderKey != null) {
            String dtraceIdFromHeader = request.getHeader(distributedTraceIdHeaderKey);
            Object dtraceIdFromAttribute = request.getAttribute(distributedTraceIdHeaderKey);
            if (StringUtils.isNotBlank(dtraceIdFromHeader))
                traceIdToUse = dtraceIdFromHeader.trim();
            else if (dtraceIdFromAttribute != null && StringUtils.isNotBlank(dtraceIdFromAttribute.toString()))
                traceIdToUse = dtraceIdFromAttribute.toString().trim();
        }

        if (traceIdToUse == null) {
            // As a last resort try to get it from the MDC since some distributed systems (e.g. Wingtips) put the
            //      trace ID there.
            String fromMdc = MDC.get(TRACE_ID_MDC_KEY);
            if (fromMdc != null)
                traceIdToUse = fromMdc.trim();
        }

        return traceIdToUse;
    }

    /**
     * Creates a UUID to use as the unique request ID for this request and attaches it to the given StringBuilder along
     * with the given request's URI, query string, distributed trace ID, request headers, and extra logging info -
     * all details are added in key=value or key="value" format, e.g. error_uid=xyz, or request_uri="some/uri/path".
     *
     * @return The UUID request ID that was added to the log message - this should be put into the response headers and
     *          response body so that you can trivially go from the response to the log message that has all the
     *          debugging info.
     */
    public String buildErrorMessageForLogs(StringBuilder sb, RequestInfoForLogging request,
                                           Collection<ApiError> contributingErrors, Integer httpStatusCode,
                                           Throwable cause, List<Pair<String, String>> extraDetailsForLogging) {

        String errorUid = UUID.randomUUID().toString();
        String traceId = extractDistributedTraceId(request);
        String requestUri = request.getRequestUri();
        String requestMethod = request.getRequestHttpMethod();
        String queryString = request.getQueryString();
        String headersString = parseRequestHeadersToString(request);
        String contributingErrorsString = concatenateErrorCollection(contributingErrors);

        sb.append("error_uid=").append(errorUid)
          .append(", dtrace_id=").append(traceId)
          .append(", exception_class=").append(cause.getClass().getName())
          .append(", returned_http_status_code=").append(httpStatusCode)
          .append(", contributing_errors=\"").append(contributingErrorsString)
          .append("\", request_uri=\"").append(requestUri)
          .append("\", request_method=\"").append(requestMethod)
          .append("\", query_string=\"").append(queryString)
          .append("\", request_headers=\"").append(headersString)
          .append("\"");

        if (extraDetailsForLogging != null) {
            for (Pair<String, String> logMe : extraDetailsForLogging) {
                sb.append(", ").append(logMe.getLeft()).append("=\"").append(logMe.getRight()).append('\"');
            }
        }

        return errorUid;
    }

    /**
     * @return All the headers in the given request as a comma-separated list of name=value in string form.
     *          Multi-value headers will come back in name=[value1,value2] form.
     *          NOTE: This method never throws an exception. If it catches one it will return blank string "" instead.
     */
    public String parseRequestHeadersToString(RequestInfoForLogging request) {
        try {
            Map<String, List<String>> headers = request.getHeadersMap();
            if (headers == null || headers.isEmpty())
                return "";

            Set<String> headerNames = headers.keySet();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String headerName : headerNames) {
                if (!first)
                    sb.append(",");
                sb.append(parseSpecificHeaderToString(request, headerName));
                first = false;
            }

            return sb.toString();
        }
        catch(Exception ex) {
            return "";
        }
    }

    /**
     * @return The header(s) in the given request with the given header name as a comma-separated list in name=value or
     *          name=[value1,value2] string form, depending on whether the header with the given name has multiple
     *          values.
     *          NOTE: This method never throws an exception. If it catches one it will return blank string "" instead.
     */
    public String parseSpecificHeaderToString(RequestInfoForLogging request, String headerName) {
        try {
            if (maskSensitiveHeaders && sensitiveHeaderKeysForMasking.contains(headerName)) {
                return headerName + "=[MASKED]";
            } else {
                List<String> headerValues = request.getHeaders(headerName);
                if (headerValues == null || headerValues.isEmpty())
                    return "";

                StringBuilder sb = new StringBuilder();
                sb.append(headerName).append("=");
                // If we have more than one header for this header name, display it as an array.
                if (headerValues.size() > 1)
                    sb.append('[');

                boolean first = true;
                for (String header : headerValues) {
                    if (!first)
                        sb.append(",");
                    sb.append(header);
                    first = false;
                }

                // Close the array if appropriate
                if (headerValues.size() > 1)
                    sb.append(']');

                return sb.toString();
            }
        }
        catch(Exception ex) {
            return "";
        }
    }

    /**
     * @return Helper method for turning the given collection into a comma-delimited string of
     *          {@link ApiError#getName()}. Will return blank string (not null) if you pass in null or an empty
     *          collection.
     */
    public String concatenateErrorCollection(Collection<ApiError> errors) {
        if (errors == null || errors.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ApiError error : errors) {
            if (!first)
                sb.append(',');
            sb.append(error.getName());
            first = false;
        }

        return sb.toString();
    }

}
