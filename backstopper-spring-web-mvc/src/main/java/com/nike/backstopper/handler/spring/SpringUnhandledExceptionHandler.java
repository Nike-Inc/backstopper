package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnhandledExceptionHandlerServletApiBase;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An extension of {@link UnhandledExceptionHandlerServletApiBase} that acts as a final catch-all exception handler.
 * Translates *all* exceptions to a {@link ProjectApiErrors#getGenericServiceError()}, which is then converted
 * to a {@link ModelAndView} for the caller via
 * {@link SpringApiExceptionHandlerUtils#generateModelAndViewForErrorResponse(DefaultErrorContractDTO, int, Collection,
 * Throwable, RequestInfoForLogging)}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
@Named
@Singleton
public class SpringUnhandledExceptionHandler extends UnhandledExceptionHandlerServletApiBase<ModelAndView>
    implements HandlerExceptionResolver, Ordered {

    /**
     * The sort order for where this handler goes in the spring exception handler chain. We default to -1 so this gets
     * executed after any custom handlers, but before any default spring handlers.
     */
    private int order = -1;

    protected final SpringApiExceptionHandlerUtils springUtils;
    protected final Set<ApiError> singletonGenericServiceError;
    protected final int genericServiceErrorHttpStatusCode;

    @Inject
    public SpringUnhandledExceptionHandler(ProjectApiErrors projectApiErrors,
                                           ApiExceptionHandlerUtils generalUtils,
                                           SpringApiExceptionHandlerUtils springUtils) {
        super(projectApiErrors, generalUtils);
        this.springUtils = springUtils;
        this.singletonGenericServiceError = Collections.singleton(projectApiErrors.getGenericServiceError());
        this.genericServiceErrorHttpStatusCode = projectApiErrors.getGenericServiceError().getHttpStatusCode();
    }

    @Override
    protected ModelAndView prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO, int httpStatusCode,
                                                          Collection<ApiError> rawFilteredApiErrors,
                                                          Throwable originalException, RequestInfoForLogging request) {
        return springUtils.generateModelAndViewForErrorResponse(
            errorContractDTO, httpStatusCode, rawFilteredApiErrors, originalException, request
        );
    }

    @Override
    protected ErrorResponseInfo<ModelAndView> generateLastDitchFallbackErrorResponseInfo(
        Throwable ex, RequestInfoForLogging request, String errorUid,
        Map<String, List<String>> headersForResponseWithErrorUid
    ) {
        DefaultErrorContractDTO errorContract = new DefaultErrorContractDTO(errorUid, singletonGenericServiceError);
        return new ErrorResponseInfo<>(
            genericServiceErrorHttpStatusCode,
            // We can't trust the springUtils in this class since it could be causing the problem.
            //      Use the DEFAULT_IMPL instead.
            SpringApiExceptionHandlerUtils.DEFAULT_IMPL.generateModelAndViewForErrorResponse(
                errorContract, genericServiceErrorHttpStatusCode, singletonGenericServiceError, ex, request
            ),
            headersForResponseWithErrorUid
        );
    }

    @Override
    public ModelAndView resolveException(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        Object handler,
        @NotNull Exception ex
    ) {
        return handleException(ex, request, response).frameworkRepresentationObj;
    }

    /**
     * See the javadocs for {@link #order} for info on what this is for.
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * See the javadocs for {@link #order} for info on what this is for.
     */
    @SuppressWarnings("unused")
    public void setOrder(int order) {
        this.order = order;
    }

}
