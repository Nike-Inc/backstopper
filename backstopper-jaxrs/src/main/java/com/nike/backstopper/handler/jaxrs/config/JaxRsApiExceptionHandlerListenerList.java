package com.nike.backstopper.handler.jaxrs.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jaxrs.listener.impl.JaxRsWebApplicationExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Collection {@link Singleton} class that provides a collection of default {@link ApiExceptionHandlerListener}s.
 *
 * @author Michael Irwin
 */
@Singleton
public class JaxRsApiExceptionHandlerListenerList {

    public final List<ApiExceptionHandlerListener> listeners;

    @Inject
    @SuppressWarnings("unused")
    public JaxRsApiExceptionHandlerListenerList(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
        this(defaultApiExceptionHandlerListeners(projectApiErrors, utils));
    }

    public JaxRsApiExceptionHandlerListenerList(List<ApiExceptionHandlerListener> listeners) {
        this.listeners = listeners;
    }

    /**
     * @return The basic set of handler listeners that are appropriate for most JAX-RS applications.
     */
    public static List<ApiExceptionHandlerListener> defaultApiExceptionHandlerListeners(
        ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils
    ) {
        return Arrays.asList(
            new GenericApiExceptionHandlerListener(),
            new ServersideValidationErrorHandlerListener(projectApiErrors, utils),
            new ClientDataValidationErrorHandlerListener(projectApiErrors, utils),
            new DownstreamNetworkExceptionHandlerListener(projectApiErrors),
            new JaxRsWebApplicationExceptionHandlerListener(projectApiErrors, utils));
    }

}
