package com.nike.backstopper.jersey2sample.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jersey2.Jersey2ApiExceptionHandler;
import com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper;
import com.nike.backstopper.jersey2sample.error.SampleProjectApiErrorsImpl;
import com.nike.backstopper.jersey2sample.resource.SampleResource;
import com.nike.backstopper.service.ClientDataValidationService;

import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * This {@link ResourceConfig} will setup the Jersey 2 Sample App with a {@link Jersey2ApiExceptionHandler} for handling
 * all errors, and {@link SampleResource} for handling requests.
 *
 * <p>NOTE: There are probably better Jersey 2 idiomatic ways to wire up the dependencies than manually creating the
 * objects and passing them to {@link ResourceConfig#register(Object)}. If anyone out there is good with Jersey 2 please
 * feel free to submit a pull request.
 *
 * <p>ALSO NOTE: The hack we're doing in {@link
 * Jersey2BackstopperConfigHelper#setupJersey2ResourceConfigForBackstopperExceptionHandling(ResourceConfig,
 * ProjectApiErrors, ApiExceptionHandlerUtils)} to override the default {@link ExceptionMapperFactory} in order to make
 * sure our {@link Jersey2ApiExceptionHandler} is the only exception mapper that ever gets used is pretty ugly. There
 * may or may not be better ways to do this - https://java.net/jira/browse/JERSEY-2437 and
 * https://java.net/jira/browse/JERSEY-2722 and seem to be blockers to a clean solution, but if you have a better one
 * please feel free to submit a pull request.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class Jersey2SampleResourceConfig extends ResourceConfig {

    protected static final ProjectApiErrors projectApiErrors = new SampleProjectApiErrorsImpl();

    public Jersey2SampleResourceConfig() {
        Jersey2BackstopperConfigHelper.setupJersey2ResourceConfigForBackstopperExceptionHandling(
            this, projectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL
        );
        register(generateSampleResource());
    }

    protected SampleResource generateSampleResource() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new SampleResource(new ClientDataValidationService(validator));
    }

}
