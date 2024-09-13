package com.nike.backstopper.handler.spring.webflux.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.spring.listener.impl.OneOffSpringCommonFrameworkExceptionHandlerListener;

import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * An extension and concrete implementation of {@link OneOffSpringCommonFrameworkExceptionHandlerListener} that
 * knows how to handle Spring WebFlux specific exceptions.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class OneOffSpringWebFluxFrameworkExceptionHandlerListener
    extends OneOffSpringCommonFrameworkExceptionHandlerListener {

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} that should be used by this instance when finding {@link
     * ApiError}s. Cannot be null.
     * @param utils The {@link ApiExceptionHandlerUtils} that should be used by this instance. You can pass in
     * {@link ApiExceptionHandlerUtils#DEFAULT_IMPL} if you don't need custom logic.
     */
    @Inject
    public OneOffSpringWebFluxFrameworkExceptionHandlerListener(ProjectApiErrors projectApiErrors,
                                                                ApiExceptionHandlerUtils utils) {
        super(projectApiErrors, utils);
    }

    @Override
    protected @NotNull ApiExceptionHandlerListenerResult handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
        @NotNull Throwable ex
    ) {
        // If/when we get webflux specific exceptions, they would be handled here.

        // This exception is not handled here.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }
}
