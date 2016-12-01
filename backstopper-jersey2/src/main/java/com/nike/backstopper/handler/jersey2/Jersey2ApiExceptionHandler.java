package com.nike.backstopper.handler.jersey2;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jaxrs.JaxRsApiExceptionHandler;
import com.nike.backstopper.handler.jaxrs.JaxRsUnhandledExceptionHandler;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

import static com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.Jersey2ApiExceptionHandlerListenerList;

/**
 * A {@link JaxRsApiExceptionHandler} extension that overrides the default list of {@link ApiExceptionHandlerListener}s.
 *
 * @author Michael Irwin
 */
@Provider
@Singleton
public class Jersey2ApiExceptionHandler extends JaxRsApiExceptionHandler {

    @Inject
    public Jersey2ApiExceptionHandler(ProjectApiErrors projectApiErrors,
                                      Jersey2ApiExceptionHandlerListenerList apiExceptionHandlerListenerList,
                                      ApiExceptionHandlerUtils apiExceptionHandlerUtils,
                                      JaxRsUnhandledExceptionHandler jaxRsUnhandledExceptionHandler) {

        super(projectApiErrors, apiExceptionHandlerListenerList.listeners, apiExceptionHandlerUtils, jaxRsUnhandledExceptionHandler);
    }

}
