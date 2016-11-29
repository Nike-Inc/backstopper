package com.nike.backstopper.handler.jersey2;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerServletApiBase;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.ApiExceptionHandlerListenerList;

/**
 * An {@link ApiExceptionHandlerServletApiBase} extension that hooks into Jersey via its
 * {@link ExceptionMapper} implementation, specifically {@link ExceptionMapper#toResponse(Throwable)}.
 *
 * <p>Any errors not handled here are things we don't know how to deal with and will fall through to {@link
 * Jersey2UnhandledExceptionHandler}.
 *
 * Created by dsand7 on 9/19/14.
 */
@Provider
@Singleton
public class Jersey2ApiExceptionHandler extends ApiExceptionHandlerServletApiBase<Response.ResponseBuilder>
    implements ExceptionMapper<Throwable> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The {@link Jersey2UnhandledExceptionHandler} that handles any
     * exceptions we weren't anticipating
     */
    @SuppressWarnings("WeakerAccess")
    protected final Jersey2UnhandledExceptionHandler jerseyUnhandledExceptionHandler;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Inject
    public Jersey2ApiExceptionHandler(ProjectApiErrors projectApiErrors,
                                      ApiExceptionHandlerListenerList apiExceptionHandlerListenerList,
                                      ApiExceptionHandlerUtils apiExceptionHandlerUtils,
                                      Jersey2UnhandledExceptionHandler jerseyUnhandledExceptionHandler) {

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
                                 + Jersey2UnhandledExceptionHandler.class.getName() + " should handle it.");
                }
                exceptionHandled = jerseyUnhandledExceptionHandler.handleException(e, request, response);
            }

        }
        catch (UnexpectedMajorExceptionHandlingError ohNoException) {
            logger.error("Unexpected major error while handling exception. " +
                         Jersey2UnhandledExceptionHandler.class.getName() + " should handle it.", ohNoException);
            exceptionHandled = jerseyUnhandledExceptionHandler.handleException(e, request, response);
        }

        Response.ResponseBuilder responseBuilder = exceptionHandled.frameworkRepresentationObj
            .header("Content-Type", MediaType.APPLICATION_JSON);

        // NOTE: We don't have to add headers to the response here - it's already been done in the
        //      ApiExceptionHandlerServletApiBase.processServletResponse(...) method.

        return responseBuilder.build();
    }
}
