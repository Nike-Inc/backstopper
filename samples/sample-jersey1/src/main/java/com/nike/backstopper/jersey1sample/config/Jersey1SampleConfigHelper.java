package com.nike.backstopper.jersey1sample.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jersey1.Jersey1ApiExceptionHandler;
import com.nike.backstopper.handler.jersey1.Jersey1UnhandledExceptionHandler;
import com.nike.backstopper.handler.jersey1.config.Jersey1BackstopperConfigHelper;
import com.nike.backstopper.jersey1sample.error.SampleProjectApiErrorsImpl;
import com.nike.backstopper.jersey1sample.resource.SampleResource;
import com.nike.backstopper.service.ClientDataValidationService;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * This Jersey 1 sample app is as barebones as we could make it, which means no dependency injection. This class
 * is here to provide instances of {@link Jersey1ApiExceptionHandler} and {@link SampleResource} for registration
 * with Jersey so that they are picked up and used, but gives us the control over their creation so we
 * can manually inject them with the dependencies they need.
 *
 * <p>In a normal production Jersey app these might be auto-generated for you using dependency injection, and
 * auto-picked-up without having to manually register them.
 *
 * @author Nic Munroe
 */
public class Jersey1SampleConfigHelper {

    private static final ProjectApiErrors projectApiErrors = new SampleProjectApiErrorsImpl();

    public static Jersey1ApiExceptionHandler generateJerseyApiExceptionHandler() {
        ApiExceptionHandlerUtils utils = ApiExceptionHandlerUtils.DEFAULT_IMPL;
        Jersey1BackstopperConfigHelper.ApiExceptionHandlerListenerList
            listeners = new Jersey1BackstopperConfigHelper.ApiExceptionHandlerListenerList(
            Jersey1BackstopperConfigHelper.defaultApiExceptionHandlerListeners(projectApiErrors, utils)
        );
        Jersey1UnhandledExceptionHandler unhandledExceptionHandler =
            new Jersey1UnhandledExceptionHandler(projectApiErrors, utils);

        return new Jersey1ApiExceptionHandler(
            projectApiErrors, listeners, utils, unhandledExceptionHandler
        );
    }

    public static SampleResource generateSampleResource() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new SampleResource(new ClientDataValidationService(validator));
    }

}
