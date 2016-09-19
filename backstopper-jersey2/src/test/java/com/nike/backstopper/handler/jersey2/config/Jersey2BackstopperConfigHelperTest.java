package com.nike.backstopper.handler.jersey2.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.jersey2.Jersey2ApiExceptionHandler;
import com.nike.backstopper.handler.jersey2.Jersey2UnhandledExceptionHandler;
import com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.ApiExceptionHandlerListenerList;
import com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.BackstopperOnlyExceptionMapperFactory;
import com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.ExceptionMapperFactoryOverrideBinder;
import com.nike.backstopper.handler.jersey2.listener.impl.Jersey2WebApplicationExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;

import com.fasterxml.jackson.jaxrs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;

import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.ext.ExceptionMapper;

import static com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.defaultApiExceptionHandlerListeners;
import static com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.generateJerseyApiExceptionHandler;
import static com.nike.backstopper.handler.jersey2.config.Jersey2BackstopperConfigHelper.setupJersey2ResourceConfigForBackstopperExceptionHandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link Jersey2BackstopperConfigHelper}.
 *
 * @author Nic Munroe
 */
public class Jersey2BackstopperConfigHelperTest {

    private ProjectApiErrors projectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private ApiExceptionHandlerUtils utils = mock(ApiExceptionHandlerUtils.class);

    private void verifyContainsExpectedField(Object obj, String fieldName, Object expectedValue) {
        assertThat(Whitebox.getInternalState(obj, fieldName)).isEqualTo(expectedValue);
    }

    private void verifyDefaultListenerList(List<ApiExceptionHandlerListener> listeners) {
        assertThat(listeners).hasSize(5);

        assertThat(listeners.get(0)).isInstanceOf(GenericApiExceptionHandlerListener.class);

        assertThat(listeners.get(1)).isInstanceOf(ServersideValidationErrorHandlerListener.class);
        verifyContainsExpectedField(listeners.get(1), "projectApiErrors", projectApiErrors);
        verifyContainsExpectedField(listeners.get(1), "utils", utils);

        assertThat(listeners.get(2)).isInstanceOf(ClientDataValidationErrorHandlerListener.class);
        verifyContainsExpectedField(listeners.get(2), "projectApiErrors", projectApiErrors);
        verifyContainsExpectedField(listeners.get(2), "utils", utils);

        assertThat(listeners.get(3)).isInstanceOf(DownstreamNetworkExceptionHandlerListener.class);
        verifyContainsExpectedField(listeners.get(3), "projectApiErrors", projectApiErrors);

        assertThat(listeners.get(4)).isInstanceOf(Jersey2WebApplicationExceptionHandlerListener.class);
        verifyContainsExpectedField(listeners.get(4), "projectApiErrors", projectApiErrors);
        verifyContainsExpectedField(listeners.get(4), "utils", utils);
    }

    @Test
    public void defaultApiExceptionHandlerListeners_creates_default_list_of_listeners() {
        // when
        List<ApiExceptionHandlerListener> defaultListeners = defaultApiExceptionHandlerListeners(projectApiErrors, utils);

        // then
        verifyDefaultListenerList(defaultListeners);
    }

    private void verifyDefaultJersey2ApiExceptionHandler(Jersey2ApiExceptionHandler handler) {
        verifyDefaultListenerList(
            (List<ApiExceptionHandlerListener>) Whitebox.getInternalState(handler, "apiExceptionHandlerListenerList")
        );
        verifyContainsExpectedField(handler, "projectApiErrors", projectApiErrors);
        verifyContainsExpectedField(handler, "utils", utils);
        Jersey2UnhandledExceptionHandler unhandledHandler =
            (Jersey2UnhandledExceptionHandler) Whitebox.getInternalState(handler, "jerseyUnhandledExceptionHandler");
        verifyContainsExpectedField(unhandledHandler, "projectApiErrors", projectApiErrors);
        verifyContainsExpectedField(unhandledHandler, "utils", utils);
    }

    @Test
    public void generateJerseyApiExceptionHandler_creates_default_handler() {
        // when
        Jersey2ApiExceptionHandler handler = generateJerseyApiExceptionHandler(projectApiErrors, utils);

        // then
        verifyDefaultJersey2ApiExceptionHandler(handler);
    }

    @Test
    public void setupJersey2ResourceConfigForBackstopperExceptionHandling_sets_up_expected_defaults() {
        // given
        ResourceConfig resourceConfigMock = mock(ResourceConfig.class);

        // when
        setupJersey2ResourceConfigForBackstopperExceptionHandling(resourceConfigMock, projectApiErrors, utils);

        // then
        ArgumentCaptor<Object> registerArgCaptor = ArgumentCaptor.forClass(Object.class);
        verify(resourceConfigMock, times(2)).register(registerArgCaptor.capture());
        List<Object> registeredResources = registerArgCaptor.getAllValues();
        assertThat(registeredResources).hasSize(2);
        assertThat(registeredResources.get(0)).isInstanceOf(ExceptionMapperFactoryOverrideBinder.class);
        assertThat(registeredResources.get(1)).isInstanceOf(Jersey2ApiExceptionHandler.class);
        Jersey2ApiExceptionHandler registeredHandler = (Jersey2ApiExceptionHandler) registeredResources.get(1);
        verifyDefaultJersey2ApiExceptionHandler(registeredHandler);
    }

    @Test
    public void code_coverage_hoops() {
        // jump!
        new Jersey2BackstopperConfigHelper();
    }

    @Test
    public void apiExceptionHandlerListenerList_injector_constructor_creates_default_listener_list() {
        // when
        ApiExceptionHandlerListenerList listHolder = new ApiExceptionHandlerListenerList(projectApiErrors, utils);

        // then
        verifyDefaultListenerList(listHolder.listeners);
    }

    @Test
    public void exceptionMapperFactoryOverrideBinder_configures_ExceptionMappers_override() {
        // given
        AbstractBinder defaultJersey2ExceptionMapperBinder = new ExceptionMapperFactory.Binder();
        ExceptionMapperFactoryOverrideBinder overrideBinder =  new ExceptionMapperFactoryOverrideBinder();
        ServiceLocator locator = ServiceLocatorUtilities.bind(defaultJersey2ExceptionMapperBinder, overrideBinder);

        // when
        ExceptionMappers result = locator.getService(ExceptionMappers.class);

        // then
        assertThat(result).isInstanceOf(BackstopperOnlyExceptionMapperFactory.class);
    }

    @Test
    public void backstopperOnlyExceptionMapperFactory_removes_all_exception_mappers_except_Jersey2ApiExceptionHandler()
        throws NoSuchFieldException, IllegalAccessException {
        // given
        AbstractBinder lotsOfExceptionMappersBinder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(JsonMappingExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
                bind(JsonParseExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
                bind(generateJerseyApiExceptionHandler(projectApiErrors, utils)).to(ExceptionMapper.class);
            }
        };

        ServiceLocator locator = ServiceLocatorUtilities.bind(lotsOfExceptionMappersBinder);

        // when
        BackstopperOnlyExceptionMapperFactory overrideExceptionMapper = new BackstopperOnlyExceptionMapperFactory(locator);

        // then
        Set<Object> emTypesLeft = overrideExceptionMapper.getFieldObj(
            ExceptionMapperFactory.class, overrideExceptionMapper, "exceptionMapperTypes"
        );
        assertThat(emTypesLeft).hasSize(1);
        ServiceHandle serviceHandle = overrideExceptionMapper.getFieldObj(emTypesLeft.iterator().next(), "mapper");
        assertThat(serviceHandle.getService()).isInstanceOf(Jersey2ApiExceptionHandler.class);
    }
}