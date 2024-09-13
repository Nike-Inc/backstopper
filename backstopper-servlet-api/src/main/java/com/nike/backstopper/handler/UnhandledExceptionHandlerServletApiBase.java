package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.adapter.RequestInfoForLoggingServletApiAdapter;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple extension of {@link com.nike.backstopper.handler.UnhandledExceptionHandlerBase} that provides some convenience
 * when working in a Servlet API based framework. Implementors can call {@link #handleException(Throwable,
 * jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)} instead of {@link
 * #handleException(Throwable, RequestInfoForLogging)} to populate the servlet response's headers and status code
 * automatically.
 *
 * @author Nic Munroe
 */
public abstract class UnhandledExceptionHandlerServletApiBase<T> extends UnhandledExceptionHandlerBase<T> {

    /**
     * Creates a new instance with the given arguments.
     *
     * @param projectApiErrors The {@link ProjectApiErrors} used for this project - cannot be null.
     * @param utils            The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass
     *                         in {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    public UnhandledExceptionHandlerServletApiBase(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
        super(projectApiErrors, utils);
    }

    /**
     * Helper wrapper around {@link #handleException(Throwable, RequestInfoForLogging)} that takes in servlet request
     * and servlet response. The request will be wrapped in a {@link RequestInfoForLoggingServletApiAdapter} so that it
     * can be passed along to the method that does the work. If there are any headers in the returned {@link
     * ErrorResponseInfo#headersToAddToResponse} then they will be automatically added to the given servlet response,
     * and {@link jakarta.servlet.http.HttpServletResponse#setStatus(int)} will be automatically set with {@link
     * ErrorResponseInfo#httpStatusCode} as well.
     */
    public ErrorResponseInfo<T> handleException(Throwable ex, HttpServletRequest servletRequest,
                                                HttpServletResponse servletResponse) {

        ErrorResponseInfo<T> errorResponseInfo = handleException(
            ex, new RequestInfoForLoggingServletApiAdapter(servletRequest)
        );

        processServletResponse(errorResponseInfo, servletResponse);

        return errorResponseInfo;
    }

    @SuppressWarnings("WeakerAccess")
    protected void processServletResponse(ErrorResponseInfo<T> errorResponseInfo, HttpServletResponse servletResponse) {
        for (Map.Entry<String, List<String>> header : errorResponseInfo.headersToAddToResponse.entrySet()) {
            for (String headerValue : header.getValue()) {
                servletResponse.addHeader(header.getKey(), headerValue);
            }
        }

        servletResponse.setStatus(errorResponseInfo.httpStatusCode);
    }
}
