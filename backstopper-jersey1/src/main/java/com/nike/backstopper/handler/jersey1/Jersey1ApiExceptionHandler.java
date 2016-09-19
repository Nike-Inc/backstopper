package com.nike.backstopper.handler.jersey1;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerServletApiBase;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.jersey1.config.Jersey1BackstopperConfigHelper.ApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * An {@link ApiExceptionHandlerServletApiBase} extension that hooks into Jersey via its
 * {@link ExceptionMapper} implementation, specifically {@link ExceptionMapper#toResponse(Throwable)}.
 *
 * <p>Any errors not handled here are things we don't know how to deal with and will fall through to {@link
 * Jersey1UnhandledExceptionHandler}.
 *
 * Created by dsand7 on 9/19/14.
 */
@Provider
public class Jersey1ApiExceptionHandler extends ApiExceptionHandlerServletApiBase<Response.ResponseBuilder>
    implements ExceptionMapper<Throwable> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The {@link Jersey1UnhandledExceptionHandler} that handles any
     * exceptions we weren't anticipating
     */
    @SuppressWarnings("WeakerAccess")
    protected final Jersey1UnhandledExceptionHandler jerseyUnhandledExceptionHandler;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Inject
    public Jersey1ApiExceptionHandler(ProjectApiErrors projectApiErrors,
                                      ApiExceptionHandlerListenerList apiExceptionHandlerListenerList,
                                      ApiExceptionHandlerUtils apiExceptionHandlerUtils,
                                      Jersey1UnhandledExceptionHandler jerseyUnhandledExceptionHandler) {

        super(projectApiErrors, apiExceptionHandlerListenerList.listeners, apiExceptionHandlerUtils);

        if (jerseyUnhandledExceptionHandler == null)
            throw new IllegalArgumentException("jerseyUnhandledExceptionHandler cannot be null");

        this.jerseyUnhandledExceptionHandler = jerseyUnhandledExceptionHandler;
    }

    @Override
    protected Response.ResponseBuilder prepareFrameworkRepresentation(
        DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException, RequestInfoForLogging request
    ) {
        return Response.status(httpStatusCode).entity(
            JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(errorContractDTO));
    }

    @Override
    public Response toResponse(Throwable e) {

        ErrorResponseInfo<Response.ResponseBuilder> exceptionHandled;

        try {
            exceptionHandled = maybeHandleException(e, request, response);

            if (exceptionHandled == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No suitable handlers found for exception=" + e.getMessage() + ". "
                                 + Jersey1UnhandledExceptionHandler.class.getName() + " should handle it.");
                }
                exceptionHandled = jerseyUnhandledExceptionHandler.handleException(e, request, response);
            }

        }
        catch (UnexpectedMajorExceptionHandlingError ohNoException) {
            logger.error("Unexpected major error while handling exception. " +
                         Jersey1UnhandledExceptionHandler.class.getName() + " should handle it.", ohNoException);
            exceptionHandled = jerseyUnhandledExceptionHandler.handleException(e, request, response);
        }

        Response.ResponseBuilder responseBuilder = exceptionHandled.frameworkRepresentationObj
            .header("Content-Type", MediaType.APPLICATION_JSON);
        for (Map.Entry<String, List<String>> headerEntry : exceptionHandled.headersToAddToResponse.entrySet()) {
            for (String headerValue : headerEntry.getValue()) {
                responseBuilder = responseBuilder.header(headerEntry.getKey(), headerValue);
            }
        }

        return responseBuilder.build();
    }
}
