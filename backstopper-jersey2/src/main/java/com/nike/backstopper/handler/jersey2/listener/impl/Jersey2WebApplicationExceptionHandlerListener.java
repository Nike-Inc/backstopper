package com.nike.backstopper.handler.jersey2.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.glassfish.jersey.server.ParamException;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static com.nike.backstopper.apierror.SortedApiErrorSet.singletonSortedSetOf;

/**
 * Handles any known errors thrown by the Jersey framework.
 *
 * Created by dsand7 on 9/22/14.
 */
@Singleton
@SuppressWarnings("WeakerAccess")
public class Jersey2WebApplicationExceptionHandlerListener implements ApiExceptionHandlerListener {

    protected final ProjectApiErrors projectApiErrors;
    protected final ApiExceptionHandlerUtils utils;

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     *                         ApiError}s. Cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance.
     */
    @Inject
    public Jersey2WebApplicationExceptionHandlerListener(ProjectApiErrors projectApiErrors,
                                                         ApiExceptionHandlerUtils utils) {
        if (projectApiErrors == null)
            throw new IllegalArgumentException("ProjectApiErrors cannot be null");

        if (utils == null)
            throw new IllegalArgumentException("ApiExceptionHandlerUtils cannot be null");

        this.projectApiErrors = projectApiErrors;
        this.utils = utils;
    }

    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {

        ApiExceptionHandlerListenerResult result;
        SortedApiErrorSet handledErrors = null;
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();

        if (ex instanceof NotFoundException) {
            handledErrors = singletonSortedSetOf(projectApiErrors.getNotFoundApiError());
        }
        else if (ex instanceof ParamException.UriParamException) {
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
            // Returning a 404 is intentional here.
            //      The Jersey contract for URIParamException states it should map to a 404.
            handledErrors = singletonSortedSetOf(projectApiErrors.getNotFoundApiError());
        }
        else if (ex instanceof ParamException) {
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
            handledErrors = singletonSortedSetOf(projectApiErrors.getMalformedRequestApiError());
        }
        else if (ex instanceof WebApplicationException) {
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
            WebApplicationException webex = (WebApplicationException) ex;
            Response webExResponse = webex.getResponse();
            if (webExResponse != null) {
                int webExStatusCode = webExResponse.getStatus();
                if (webExStatusCode == HttpServletResponse.SC_NOT_ACCEPTABLE) {
                    handledErrors = singletonSortedSetOf(projectApiErrors.getNoAcceptableRepresentationApiError());
                }
                else if (webExStatusCode == HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE) {
                    handledErrors = singletonSortedSetOf(projectApiErrors.getUnsupportedMediaTypeApiError());
                }
                else if (webExStatusCode == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
                    handledErrors = singletonSortedSetOf(projectApiErrors.getMethodNotAllowedApiError());
                }
                else if (webExStatusCode == HttpServletResponse.SC_UNAUTHORIZED) {
                    handledErrors = singletonSortedSetOf(projectApiErrors.getUnauthorizedApiError());
                }
            }
        }
        else if (ex instanceof JsonProcessingException) {
            utils.addBaseExceptionMessageToExtraDetailsForLogging(ex, extraDetailsForLogging);
            handledErrors = singletonSortedSetOf(projectApiErrors.getMalformedRequestApiError());
        }

        // Return an indication that we will handle this exception if handledErrors got set
        if (handledErrors != null) {
            result = ApiExceptionHandlerListenerResult.handleResponse(handledErrors, extraDetailsForLogging);
        }
        else {
            result = ApiExceptionHandlerListenerResult.ignoreResponse();
        }

        return result;
    }
}
