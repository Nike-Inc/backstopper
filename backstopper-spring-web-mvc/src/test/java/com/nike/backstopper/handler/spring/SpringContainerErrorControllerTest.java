package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.servletapi.UnhandledServletContainerErrorHelper;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link SpringContainerErrorController}.
 *
 * @author Nic Munroe
 */
public class SpringContainerErrorControllerTest {

    private ProjectApiErrors projectApiErrorsMock;
    private UnhandledServletContainerErrorHelper unhandledContainerErrorHelperMock;
    private ServletRequest servletRequestMock;

    @Before
    public void beforeMethod() {
        projectApiErrorsMock = mock(ProjectApiErrors.class);
        unhandledContainerErrorHelperMock = mock(UnhandledServletContainerErrorHelper.class);
        servletRequestMock = mock(ServletRequest.class);
    }

    @Test
    public void constructor_sets_fields_as_expected() {
        // when
        SpringContainerErrorController impl = new SpringContainerErrorController(
            projectApiErrorsMock, unhandledContainerErrorHelperMock
        );

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectApiErrorsMock);
        assertThat(impl.unhandledServletContainerErrorHelper).isSameAs(unhandledContainerErrorHelperMock);
    }

    @Test
    public void constructor_throws_NPE_if_passed_null_ProjectApiErrors() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringContainerErrorController(null, unhandledContainerErrorHelperMock)
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
            () -> new SpringContainerErrorController(projectApiErrorsMock, null)
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("UnhandledServletContainerErrorHelper cannot be null.");
    }

    @Test
    public void error_method_throws_result_of_calling_UnhandledServletContainerErrorHelper() {
        // given
        SpringContainerErrorController impl = new SpringContainerErrorController(
            projectApiErrorsMock, unhandledContainerErrorHelperMock
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