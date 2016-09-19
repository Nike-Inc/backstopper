package com.nike.backstopper.handler.jersey1.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jersey1.listener.impl.Jersey1WebApplicationExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;

import java.util.Arrays;
import java.util.List;

/**
 * Helper utility class for generating a default list of {@link ApiExceptionHandlerListener} from a project's
 * {@link ProjectApiErrors} and {@link ApiExceptionHandlerUtils}.
 *
 * Created by dsand7 on 9/22/14.
 */
public class Jersey1BackstopperConfigHelper {

    /**
     * @return The basic set of handler listeners that are appropriate for most applications.
     */
    public static List<ApiExceptionHandlerListener> defaultApiExceptionHandlerListeners(
        ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils
    ) {
        return Arrays.asList(
            new GenericApiExceptionHandlerListener(),
            new ServersideValidationErrorHandlerListener(projectApiErrors, utils),
            new ClientDataValidationErrorHandlerListener(projectApiErrors, utils),
            new DownstreamNetworkExceptionHandlerListener(projectApiErrors),
            new Jersey1WebApplicationExceptionHandlerListener(projectApiErrors, utils));
    }

    /**
     * Necessary because dependency injection of a collection as a bean doesn't work the way you think it should.
     */
    public static class ApiExceptionHandlerListenerList {
        public final List<ApiExceptionHandlerListener> listeners;

        public ApiExceptionHandlerListenerList(List<ApiExceptionHandlerListener> listeners) {
            this.listeners = listeners;
        }
    }
}
