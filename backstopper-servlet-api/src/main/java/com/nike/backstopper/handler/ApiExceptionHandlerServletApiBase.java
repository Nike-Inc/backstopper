package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.adapter.RequestInfoForLoggingServletApiAdapter;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple extension of {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} that provides some convenience when
 * working in a Servlet API based framework. Implementors can call {@link #maybeHandleException(Throwable,
 * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} instead of {@link
 * #maybeHandleException(Throwable, RequestInfoForLogging)} to populate the servlet response's headers and status code
 * automatically.
 *
 * @author Nic Munroe
 */
public abstract class ApiExceptionHandlerServletApiBase<T> extends ApiExceptionHandlerBase<T> {

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
     * @param apiExceptionHandlerUtils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You
     *                                 can pass in {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need
     *                                 custom logic. Cannot be null.
     */
    public ApiExceptionHandlerServletApiBase(ProjectApiErrors projectApiErrors,
                                             List<ApiExceptionHandlerListener> apiExceptionHandlerListenerList,
                                             ApiExceptionHandlerUtils apiExceptionHandlerUtils) {
        super(projectApiErrors, apiExceptionHandlerListenerList, apiExceptionHandlerUtils);
    }

    /**
     * Helper wrapper around {@link #maybeHandleException(Throwable, RequestInfoForLogging)} that takes in servlet
     * request and servlet response. The request will be wrapped in a {@link RequestInfoForLoggingServletApiAdapter} so
     * that it can be passed along to the method that does the work. If there are any headers in the returned {@link
     * ErrorResponseInfo#headersToAddToResponse} then they will be automatically added to the given servlet response,
     * and {@link javax.servlet.http.HttpServletResponse#setStatus(int)} will be automatically set with {@link
     * ErrorResponseInfo#httpStatusCode} as well.
     */
    public ErrorResponseInfo<T> maybeHandleException(
        Throwable ex, HttpServletRequest servletRequest, HttpServletResponse servletResponse
    ) throws UnexpectedMajorExceptionHandlingError {

        ErrorResponseInfo<T> errorResponseInfo = maybeHandleException(
            ex, new RequestInfoForLoggingServletApiAdapter(servletRequest)
        );

        if (errorResponseInfo != null)
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
