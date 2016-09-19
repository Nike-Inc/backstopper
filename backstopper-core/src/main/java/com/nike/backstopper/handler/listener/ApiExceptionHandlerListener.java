package com.nike.backstopper.handler.listener;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.ApiExceptionHandlerBase;

/**
 * An exception handler listener intended to be used by {@link ApiExceptionHandlerBase}. A concrete implementation of
 * this will look for exceptions matching some criteria and return a {@link ApiExceptionHandlerListenerResult}
 * representing either an "I want to handle this exception by using these {@link ApiError}s and this logging info"
 * result or an "I don't want to handle this exception" result.
 *
 * @author Nic Munroe
 */
public interface ApiExceptionHandlerListener {

    /**
     * @return A {@link ApiExceptionHandlerListenerResult} representing whether or not this instance wishes to handle
     *          the exception. If {@link ApiExceptionHandlerListenerResult#shouldHandleResponse} is true then
     *          {@link ApiExceptionHandlerBase} should handle the exception by using
     *          {@link ApiExceptionHandlerListenerResult#errors} for the info returned to the client and should log the
     *          info from {@link ApiExceptionHandlerListenerResult#extraDetailsForLogging}. Otherwise if
     *          {@link ApiExceptionHandlerListenerResult#shouldHandleResponse} is false then
     *          {@link ApiExceptionHandlerBase} should move on to the next listener.
     */
    ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex);

}
