package com.nike.backstopper.handler.jersey2.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jaxrs.JaxRsUnhandledExceptionHandler;
import com.nike.backstopper.handler.jaxrs.listener.impl.JaxRsWebApplicationExceptionHandlerListener;
import com.nike.backstopper.handler.jersey2.Jersey2ApiExceptionHandler;
import com.nike.backstopper.handler.jersey2.listener.impl.Jersey2WebApplicationExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;

import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ExceptionMappers;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper utility class for dealing with setting up a Jersey 2 project with Backstopper. Important methods and inner
 * classes in this class:
 *
 * <ul>
 *     <li>
 *         {@link #setupJersey2ResourceConfigForBackstopperExceptionHandling(ResourceConfig, ProjectApiErrors,
 *         ApiExceptionHandlerUtils)} - This is the main entry point for setting up a Jersey 2 app. It takes
 *         a {@link ResourceConfig}, your project's {@link ProjectApiErrors}, and the {@link ApiExceptionHandlerUtils}
 *         you want to use and generates the {@link Jersey2ApiExceptionHandler} that should be used as the
 *         one and only exception mapper for your Jersey 2 project. As part of this setup it registers an
 *         {@link ExceptionMapperFactoryOverrideBinder} that causes the Jersey 2 {@link ExceptionMapperFactory}
 *         to be limited to *only* {@link Jersey2ApiExceptionHandler}.
 *     </li>
 *     <li>
 *         {@link #generateJerseyApiExceptionHandler(ProjectApiErrors, ApiExceptionHandlerUtils)} - If you want
 *         to piecemeal your config you can use this method to generate a {@link Jersey2ApiExceptionHandler}
 *         with the given {@link ProjectApiErrors} and {@link ApiExceptionHandlerUtils}. You'd be responsible
 *         for registering this with your Jersey 2 app yourself.
 *     </li>
 *     <li>
 *         {@link #defaultApiExceptionHandlerListeners(ProjectApiErrors, ApiExceptionHandlerUtils)} - Use this method
 *         if you want even further control over creating your {@link Jersey2ApiExceptionHandler} (e.g. for overriding
 *         method behavior) but still want the default set of {@link ApiExceptionHandlerListener}s.
 *     </li>
 *     <li>
 *         {@link ExceptionMapperFactoryOverrideBinder} - Register this with your {@link ResourceConfig} to force
 *         your Jersey 2 app to *only* use {@link Jersey2ApiExceptionHandler} for exception mapping. If you don't
 *         do this then some exceptions will fall through the Backstopper net and be returned to the caller using
 *         whatever mapper it happened to pick, potentially leading to ugly non-standard error contracts and
 *         information like stack traces leaking to the caller.
 *     </li>
 * </ul>
 *
 * <p>NOTE: There are probably better Jersey 2 idiomatic ways to wire up the dependencies than manually creating
 * the objects and passing them to {@link ResourceConfig#register(Object)}. If anyone out there is good with
 * Jersey 2 please feel free to submit a pull request.
 *
 * <p>ALSO NOTE: The hack we're doing here to override the default {@link ExceptionMapperFactory} in order to make
 * sure our {@link Jersey2ApiExceptionHandler} is the only exception mapper that ever gets used is pretty ugly.
 * There may or may not be better ways to do this - https://java.net/jira/browse/JERSEY-2437 and
 * https://java.net/jira/browse/JERSEY-2722 and seem to be blockers to a clean solution, but if you have a
 * better one please feel free to submit a pull request.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class Jersey2BackstopperConfigHelper {

    /**
     * Generates a default {@link Jersey2ApiExceptionHandler} (by calling {@link
     * #generateJerseyApiExceptionHandler(ProjectApiErrors, ApiExceptionHandlerUtils)}) as well as a {@link
     * ExceptionMapperFactoryOverrideBinder} and registers them both with the given resource config using {@link
     * ResourceConfig#register(Object)}.
     *
     * @param resourceConfig The resource config to register with.
     * @param projectApiErrors The {@link ProjectApiErrors} for your project.
     * @param utils The {@link ApiExceptionHandlerUtils} you want to use with your project.
     */
    public static void setupJersey2ResourceConfigForBackstopperExceptionHandling(
        ResourceConfig resourceConfig, ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils
    ) {
        resourceConfig.register(new ExceptionMapperFactoryOverrideBinder());
        resourceConfig.register(generateJerseyApiExceptionHandler(projectApiErrors, utils));
    }

    /**
     * @param projectApiErrors The {@link ProjectApiErrors} for your project.
     * @param utils The {@link ApiExceptionHandlerUtils} you want to use with your project.
     * @return A {@link Jersey2ApiExceptionHandler} that uses the given arguments and contains the default set of
     * listeners (generated using {@link #defaultApiExceptionHandlerListeners(ProjectApiErrors,
     * ApiExceptionHandlerUtils)}) and a default {@link JaxRsUnhandledExceptionHandler}.
     */
    public static Jersey2ApiExceptionHandler generateJerseyApiExceptionHandler(ProjectApiErrors projectApiErrors,
                                                                               ApiExceptionHandlerUtils utils) {

        Jersey2ApiExceptionHandlerListenerList listeners = new Jersey2ApiExceptionHandlerListenerList(
            defaultApiExceptionHandlerListeners(projectApiErrors, utils)
        );

        JaxRsUnhandledExceptionHandler unhandledExceptionHandler = new JaxRsUnhandledExceptionHandler(
            projectApiErrors, utils
        );

        return new Jersey2ApiExceptionHandler(projectApiErrors, listeners, utils, unhandledExceptionHandler);
    }

    /**
     * @return The basic set of handler listeners that are appropriate for most Jersey 2 applications.
     */
    public static List<ApiExceptionHandlerListener> defaultApiExceptionHandlerListeners(
        ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils
    ) {
        return Arrays.asList(
            new GenericApiExceptionHandlerListener(),
            new ServersideValidationErrorHandlerListener(projectApiErrors, utils),
            new ClientDataValidationErrorHandlerListener(projectApiErrors, utils),
            new DownstreamNetworkExceptionHandlerListener(projectApiErrors),
            new Jersey2WebApplicationExceptionHandlerListener(projectApiErrors, utils),
            new JaxRsWebApplicationExceptionHandlerListener(projectApiErrors, utils));
    }

    /**
     * This wrapper class is necessary because dependency injection of a collection as a bean doesn't work the way you
     * think it should.
     */
    @Singleton
    public static class Jersey2ApiExceptionHandlerListenerList {
        public final List<ApiExceptionHandlerListener> listeners;

        @Inject
        @SuppressWarnings("unused")
        public Jersey2ApiExceptionHandlerListenerList(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
            this(defaultApiExceptionHandlerListeners(projectApiErrors, utils));
        }

        public Jersey2ApiExceptionHandlerListenerList(List<ApiExceptionHandlerListener> listeners) {
            this.listeners = listeners;
        }
    }

    /**
     * A special binder that you can register with your Jersey 2 {@link ResourceConfig} to replace the default
     * {@link ExceptionMapperFactory} dependency injection binding with one that creates a
     * {@link BackstopperOnlyExceptionMapperFactory} instead. This results in your Jersey 2 app only having
     * access to a {@link Jersey2ApiExceptionHandler} for mapping exceptions, which in turn guarantees
     * the Backstopper error contract is the only one callers will ever see.
     * <p>
     * NOTE: Due to the nature of how Jersey 2 starts up, this binder must be registered with the Jersey 2 app's
     * {@link ResourceConfig} *before* Jersey 2 initializes. Otherwise the default {@link ExceptionMapperFactory}
     * will be used and this binder will have no effect.
     */
    public static class ExceptionMapperFactoryOverrideBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(BackstopperOnlyExceptionMapperFactory.class)
                .to(ExceptionMappers.class)
                .in(Singleton.class)
                .ranked(Integer.MAX_VALUE);
        }
    }

    /**
     * A custom {@link ExceptionMapperFactory} that removes all exception handlers from its collection except
     * {@link Jersey2ApiExceptionHandler}. You can use {@link ExceptionMapperFactoryOverrideBinder} to override
     * Jersey 2's default {@link ExceptionMapperFactory} to use this instead so that it will only ever use
     * the Backstopper error handler, which in turn guarantees the Backstopper error contract is the only one callers
     * will ever see.
     */
    @SuppressWarnings("WeakerAccess")
    public static class BackstopperOnlyExceptionMapperFactory extends ExceptionMapperFactory {

        @Inject
        public BackstopperOnlyExceptionMapperFactory(ServiceLocator locator) throws NoSuchFieldException,
                                                                                    IllegalAccessException {
            super(locator);
            hackFixExceptionMappers();
        }

        protected void hackFixExceptionMappers()
            throws NoSuchFieldException, IllegalAccessException {
            Set<Object> exceptionMapperTypes = getFieldObj(ExceptionMapperFactory.class, this, "exceptionMapperTypes");
            Iterator<Object> exceptionMapperIterator = exceptionMapperTypes.iterator();
            while (exceptionMapperIterator.hasNext()) {
                Object mapperType = exceptionMapperIterator.next();
                ServiceHandle serviceHandle = getFieldObj(mapperType, "mapper");
                if (!(serviceHandle.getService() instanceof Jersey2ApiExceptionHandler))
                    exceptionMapperIterator.remove();
            }
        }

        protected <T> T getFieldObj(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
            return getFieldObj(obj.getClass(), obj, fieldName);
        }

        protected <T> T getFieldObj(Class<?> declaringClass,
                                  Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
            Field field = declaringClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            //noinspection unchecked
            return (T)field.get(obj);
        }
    }
}
