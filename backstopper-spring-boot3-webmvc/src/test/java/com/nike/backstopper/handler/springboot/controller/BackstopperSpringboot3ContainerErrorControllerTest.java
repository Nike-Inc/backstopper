package com.nike.backstopper.handler.springboot.controller;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.servletapi.UnhandledServletContainerErrorHelper;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.util.UUID;

import jakarta.servlet.ServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link BackstopperSpringboot3ContainerErrorController}.
 *
 * @author Nic Munroe
 */
public class BackstopperSpringboot3ContainerErrorControllerTest {

    private ProjectApiErrors projectApiErrorsMock;
    private UnhandledServletContainerErrorHelper unhandledContainerErrorHelperMock;
    private ServletRequest servletRequestMock;
    private ServerProperties serverPropertiesMock;
    private ErrorProperties errorPropertiesMock;
    private String errorPath;

    @Before
    public void beforeMethod() {
        projectApiErrorsMock = mock(ProjectApiErrors.class);
        unhandledContainerErrorHelperMock = mock(UnhandledServletContainerErrorHelper.class);
        servletRequestMock = mock(ServletRequest.class);
        serverPropertiesMock = mock(ServerProperties.class);
        errorPropertiesMock = mock(ErrorProperties.class);
        errorPath = UUID.randomUUID().toString();

        doReturn(errorPropertiesMock).when(serverPropertiesMock).getError();
        doReturn(errorPath).when(errorPropertiesMock).getPath();
    }

    @Test
    public void constructor_sets_fields_as_expected() {
        // when
        BackstopperSpringboot3ContainerErrorController impl = new BackstopperSpringboot3ContainerErrorController(
            projectApiErrorsMock, unhandledContainerErrorHelperMock, serverPropertiesMock
        );

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectApiErrorsMock);
        assertThat(impl.unhandledServletContainerErrorHelper).isSameAs(unhandledContainerErrorHelperMock);
        assertThat(impl.errorPath).isEqualTo(errorPath);
        assertThat(impl.getErrorPath()).isEqualTo(errorPath);
    }

    @Test
    public void constructor_throws_NPE_if_passed_null_ProjectApiErrors() {
        // when
        Throwable ex = catchThrowable(
            () -> new BackstopperSpringboot3ContainerErrorController(
                null, unhandledContainerErrorHelperMock, serverPropertiesMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ProjectApiErrors cannot be null.");
    }

    @Test
    public void constructor_throws_NPE_if_passed_null_UnhandledServletContainerErrorHelper() {
        // when
        Throwable ex = catchThrowable(
            () -> new BackstopperSpringboot3ContainerErrorController(
                projectApiErrorsMock, null, serverPropertiesMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("UnhandledServletContainerErrorHelper cannot be null.");
    }

    @Test
    public void constructor_throws_NPE_if_passed_null_ServerProperties() {
        // when
        Throwable ex = catchThrowable(
            () -> new BackstopperSpringboot3ContainerErrorController(
                projectApiErrorsMock, unhandledContainerErrorHelperMock, null
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ServerProperties cannot be null.");
    }

    @Test
    public void error_method_throws_result_of_calling_UnhandledServletContainerErrorHelper() {
        // given
        BackstopperSpringboot3ContainerErrorController impl = new BackstopperSpringboot3ContainerErrorController(
            projectApiErrorsMock, unhandledContainerErrorHelperMock, serverPropertiesMock
        );
        Throwable expectedEx = new RuntimeException("intentional test exception");
        doReturn(expectedEx).when(unhandledContainerErrorHelperMock)
                            .extractOrGenerateErrorForRequest(servletRequestMock, projectApiErrorsMock);

        // when
        Throwable ex = catchThrowable(() -> impl.error(servletRequestMock));

        // then
        assertThat(ex).isSameAs(expectedEx);
        verify(unhandledContainerErrorHelperMock)
            .extractOrGenerateErrorForRequest(servletRequestMock, projectApiErrorsMock);
    }

}
