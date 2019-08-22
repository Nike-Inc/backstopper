package com.nike.backstopper.handler.spring.webflux.listener;

import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;
import com.nike.backstopper.handler.spring.listener.impl.ConventionBasedSpringValidationErrorToApiErrorHandlerListener;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxApiExceptionHandler;
import com.nike.backstopper.handler.spring.webflux.listener.impl.OneOffSpringWebFluxFrameworkExceptionHandlerListener;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Specifies the list of {@link ApiExceptionHandlerListener}s that should be available to
 * {@link SpringWebfluxApiExceptionHandler}. This wrapper class is necessary because
 * direct dependency injection of a collection as a bean doesn't work in a very intuitive way.
 */
@Named
@Singleton
public class SpringWebFluxApiExceptionHandlerListenerList {

    public final List<ApiExceptionHandlerListener> listeners;

    @Inject
    @SuppressWarnings("unused")
    public SpringWebFluxApiExceptionHandlerListenerList(
        GenericApiExceptionHandlerListener genericApiExceptionHandlerListener,
        ServersideValidationErrorHandlerListener serversideValidationErrorHandlerListener,
        ClientDataValidationErrorHandlerListener clientDataValidationErrorHandlerListener,
        ConventionBasedSpringValidationErrorToApiErrorHandlerListener conventionBasedSpringValidationErrorToApiErrorHandlerListener,
        OneOffSpringWebFluxFrameworkExceptionHandlerListener oneOffSpringWebFluxFrameworkExceptionHandlerListener,
        DownstreamNetworkExceptionHandlerListener downstreamNetworkExceptionHandlerListener
    ) {
        this(Arrays.asList(
            genericApiExceptionHandlerListener,
            serversideValidationErrorHandlerListener,
            clientDataValidationErrorHandlerListener,
            conventionBasedSpringValidationErrorToApiErrorHandlerListener,
            oneOffSpringWebFluxFrameworkExceptionHandlerListener,
            downstreamNetworkExceptionHandlerListener
        ));
    }

    public SpringWebFluxApiExceptionHandlerListenerList(List<ApiExceptionHandlerListener> listeners) {
        this.listeners = listeners;
    }
}
