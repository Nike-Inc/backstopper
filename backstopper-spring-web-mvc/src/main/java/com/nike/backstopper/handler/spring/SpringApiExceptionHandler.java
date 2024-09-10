package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerServletApiBase;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.spring.listener.ApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An {@link ApiExceptionHandlerServletApiBase} extension that hooks into Spring Web MVC via its
 * {@link HandlerExceptionResolver}, and specifically {@link
 * HandlerExceptionResolver#resolveException(HttpServletRequest, HttpServletResponse, Object, Exception)}.
 *
 * <p>Any errors not handled here are things we don't know how to deal with and will fall through to {@link
 * SpringUnhandledExceptionHandler}.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
public class SpringApiExceptionHandler extends ApiExceptionHandlerServletApiBase<ModelAndView>
    implements HandlerExceptionResolver, Ordered {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The sort order for where this handler goes in the spring exception handler chain. We default to {@link
     * Ordered#HIGHEST_PRECEDENCE} plus one, so that this is tried first before any other handlers but after
     * Springboot's {@code DefaultErrorAttributes} (or any other similar handler that adds some error context info to
     * the servlet request attributes but doesn't actually handle the error).
     */
    private int order = Ordered.HIGHEST_PRECEDENCE + 1;

    @SuppressWarnings("WeakerAccess")
    protected final SpringApiExceptionHandlerUtils springUtils;

    @Inject
    public SpringApiExceptionHandler(ProjectApiErrors projectApiErrors,
                                     ApiExceptionHandlerListenerList apiExceptionHandlerListeners,
                                     ApiExceptionHandlerUtils generalUtils,
                                     SpringApiExceptionHandlerUtils springUtils) {
        super(projectApiErrors, apiExceptionHandlerListeners.listeners, generalUtils);
        this.springUtils = springUtils;
    }

    @Override
    protected ModelAndView prepareFrameworkRepresentation(
        DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException, RequestInfoForLogging request
    ) {
        return springUtils.generateModelAndViewForErrorResponse(
            errorContractDTO, httpStatusCode, rawFilteredApiErrors, originalException, request
        );
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
                                         Exception ex) {

        try {
            ErrorResponseInfo<ModelAndView> errorResponseInfo = maybeHandleException(ex, request, response);

            if (errorResponseInfo == null) {
                return null;
            }

            return errorResponseInfo.frameworkRepresentationObj;
        } catch (UnexpectedMajorExceptionHandlingError ohNoException) {
            logger.error(
                "Unexpected major error while handling exception. " + SpringUnhandledExceptionHandler.class.getName()
                + " should handle it.", ohNoException);
            return null;
        }

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
    @SuppressWarnings({"unused"})
    public void setOrder(int order) {
        this.order = order;
    }
}
