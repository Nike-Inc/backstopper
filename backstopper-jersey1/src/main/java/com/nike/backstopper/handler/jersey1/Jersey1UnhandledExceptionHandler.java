package com.nike.backstopper.handler.jersey1;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnhandledExceptionHandlerServletApiBase;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

/**
 * An extension of {@link UnhandledExceptionHandlerServletApiBase} that acts as a final catch-all exception handler.
 * Translates *all* exceptions to a {@link ProjectApiErrors#getGenericServiceError()}, which is then converted
 * to a {@link Response.ResponseBuilder} for the caller with a response entity payload built by
 * {@link JsonUtilWithDefaultErrorContractDTOSupport#writeValueAsString(Object)}.
 *
 * Created by dsand7 on 9/23/14.
 */
public class Jersey1UnhandledExceptionHandler extends UnhandledExceptionHandlerServletApiBase<Response.ResponseBuilder> {

    protected final Set<ApiError> singletonGenericServiceError;
    protected final int genericServiceErrorHttpStatusCode;

    /**
     * Creates a new instance with the given arguments.
     *
     * @param projectApiErrors The {@link ProjectApiErrors} used for this project - cannot be null.
     * @param utils            The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass
     *                         in {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    public Jersey1UnhandledExceptionHandler(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
        super(projectApiErrors, utils);
        this.singletonGenericServiceError = Collections.singleton(projectApiErrors.getGenericServiceError());
        this.genericServiceErrorHttpStatusCode = projectApiErrors.getGenericServiceError().getHttpStatusCode();
    }

    @Override
    protected Response.ResponseBuilder prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO,
                                                                      int httpStatusCode,
                                                                      Collection<ApiError> rawFilteredApiErrors,
                                                                      Throwable originalException,
                                                                      RequestInfoForLogging request) {

        return Response.status(httpStatusCode).entity(
            JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(errorContractDTO));
    }

    @Override
    protected ErrorResponseInfo<Response.ResponseBuilder> generateLastDitchFallbackErrorResponseInfo(Throwable ex,
                                                                                                     RequestInfoForLogging request,
                                                                                                     String errorUid,
                                                                                                     Map<String, List<String>> headersForResponseWithErrorUid) {
        DefaultErrorContractDTO errorContract = new DefaultErrorContractDTO(errorUid, singletonGenericServiceError);
        return new ErrorResponseInfo<>(
            genericServiceErrorHttpStatusCode,
            Response.status(genericServiceErrorHttpStatusCode).entity(
                JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(errorContract)
            ),
            headersForResponseWithErrorUid
        );
    }
}
