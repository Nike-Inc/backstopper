package com.nike.backstopper.handler.spring.listener;

import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;
import com.nike.backstopper.handler.spring.listener.impl.ConventionBasedSpringValidationErrorToApiErrorHandlerListener;
import com.nike.backstopper.handler.spring.listener.impl.OneOffSpringWebMvcFrameworkExceptionHandlerListener;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Specifies the list of {@link ApiExceptionHandlerListener}s that should be available to
 * {@link com.nike.backstopper.handler.spring.SpringApiExceptionHandler}. This wrapper class is necessary because
 * direct dependency injection of a collection as a bean doesn't work in a very intuitive way.
 */
@Named
@Singleton
public class ApiExceptionHandlerListenerList {

    public final List<ApiExceptionHandlerListener> listeners;

    @Inject
    @SuppressWarnings("unused")
    public ApiExceptionHandlerListenerList(
        GenericApiExceptionHandlerListener genericApiExceptionHandlerListener,
        ServersideValidationErrorHandlerListener serversideValidationErrorHandlerListener,
        ClientDataValidationErrorHandlerListener clientDataValidationErrorHandlerListener,
        ConventionBasedSpringValidationErrorToApiErrorHandlerListener conventionBasedSpringValidationErrorToApiErrorHandlerListener,
        OneOffSpringWebMvcFrameworkExceptionHandlerListener oneOffSpringWebMvcFrameworkExceptionHandlerListener,
        DownstreamNetworkExceptionHandlerListener downstreamNetworkExceptionHandlerListener
    ) {
        this(Arrays.asList(
            genericApiExceptionHandlerListener,
            serversideValidationErrorHandlerListener,
            clientDataValidationErrorHandlerListener,
            conventionBasedSpringValidationErrorToApiErrorHandlerListener,
            oneOffSpringWebMvcFrameworkExceptionHandlerListener,
            downstreamNetworkExceptionHandlerListener
        ));
    }

    public ApiExceptionHandlerListenerList(List<ApiExceptionHandlerListener> listeners) {
        this.listeners = listeners;
    }
}
