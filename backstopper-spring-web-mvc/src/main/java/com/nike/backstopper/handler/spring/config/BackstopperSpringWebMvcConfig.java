package com.nike.backstopper.handler.spring.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;
import com.nike.backstopper.handler.ApiExceptionHandlerBase;
import com.nike.backstopper.handler.spring.SpringApiExceptionHandler;
import com.nike.backstopper.handler.spring.SpringApiExceptionHandlerUtils;
import com.nike.backstopper.handler.spring.SpringUnhandledExceptionHandler;
import com.nike.backstopper.handler.spring.listener.ApiExceptionHandlerListenerList;
import com.nike.backstopper.service.ClientDataValidationService;
import com.nike.backstopper.service.FailFastServersideValidationService;
import com.nike.backstopper.service.NoOpJsr303Validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.validation.Validator;

/**
 * This Spring Web MVC configuration is an alternative to simply scanning all of {@code com.nike.backstopper}. You can
 * import this spring config into your main spring config with the {@link Import} annotation to enable {@link
 * SpringApiExceptionHandler} and {@link SpringUnhandledExceptionHandler} in your application. These two exception
 * handlers will supersede the built-in spring exception handler chain and will translate <b>ALL</b> errors heading to
 * the caller so that they conform to the API error contract. See the {@link SpringApiExceptionHandler} and {@link
 * SpringUnhandledExceptionHandler} classes themselves for more info.
 *
 * <p>Most of the necessary dependencies are setup for autowiring so this should be sufficient to enable Backstopper
 * error handling in your Spring Web MVC application, except for two things:
 * <ol>
 *     <li>
 *         Backstopper needs to know what your {@link ProjectApiErrors} is. You must expose an instance of that class
 *         as a dependency-injectable bean (e.g. using {@link Bean} in your Spring Web MVC config). See the javadocs
 *         for {@link ProjectApiErrors} for more information, and {@link SampleProjectApiErrorsBase} for an example base
 *         class that sets up all the core errors. Feel free to extend {@link SampleProjectApiErrorsBase} and use it
 *         directly if the error codes and messages of the core errors it provides are ok for your application).
 *     </li>
 *     <li>
 *         The {@link ClientDataValidationService} and {@link FailFastServersideValidationService} JSR 303 utility
 *         services need an injected reference to a {@link Validator}. If you have a JSR 303 Bean Validation
 *         implementation on your classpath you can just expose that (e.g. via {@link Bean}), otherwise if you
 *         don't need or want the functionality those services provide you can simply expose
 *         {@link NoOpJsr303Validator#SINGLETON_IMPL} as your {@link Validator}. Those services would then be
 *         useless, however if you're not going to use them anyway this would allow you to satisfy the dependency
 *         injection requirements without pulling in extra jars into your application just to get a {@link Validator}
 *         impl.
 *     </li>
 * </ol>
 *
 * <p>There are a few critical extension points in Backstopper that you might want to know about for fine tuning what
 * errors Backstopper knows how to handle and how your error contract looks. In particular if you want a different set
 * of handler listeners for {@link SpringApiExceptionHandler} you should specify a custom {@link
 * ApiExceptionHandlerListenerList} bean to override the default. And if you want to change how the final error contract
 * is serialized (and/or what's inside it) for the caller you can specify a custom {@link
 * SpringApiExceptionHandlerUtils}. There are other extension points for other behavior as well - Backstopper is
 * designed to be customizable.
 */
@Configuration
@ComponentScan(basePackageClasses = {
    ApiExceptionHandlerBase.class, // Covers the core exception handler base classes, utils, and listener subpackages.
    ClientDataValidationService.class, // Covers the JSR 303 service additions.
    SpringApiExceptionHandler.class // Covers the Spring Web MVC additions and subclasses.
})
public class BackstopperSpringWebMvcConfig {

}
