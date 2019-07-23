package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.spring.listener.ApiExceptionHandlerListenerList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link SpringApiExceptionHandler}.
 *
 * @author Nic Munroe
 */
public class SpringApiExceptionHandlerTest {

    private SpringApiExceptionHandler handlerSpy;
    private ProjectApiErrors projectApiErrorsMock;
    private ApiExceptionHandlerListenerList listenerList;
    private ApiExceptionHandlerUtils generalUtils;
    private SpringApiExceptionHandlerUtils springUtils;

    @Before
    public void beforeMethod() {
        projectApiErrorsMock = mock(ProjectApiErrors.class);
        listenerList = new ApiExceptionHandlerListenerList(Collections.<ApiExceptionHandlerListener>emptyList());
        generalUtils = ApiExceptionHandlerUtils.DEFAULT_IMPL;
        springUtils = SpringApiExceptionHandlerUtils.DEFAULT_IMPL;
        handlerSpy = spy(new SpringApiExceptionHandler(projectApiErrorsMock, listenerList, generalUtils, springUtils));
    }

    @Test
    public void resolveException_returns_null_if_maybeHandleException_returns_null()
        throws UnexpectedMajorExceptionHandlingError {
        // given
        doReturn(null).when(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));

        // when
        ModelAndView result = handlerSpy.resolveException(null, null, null, null);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(result).isNull();
    }

    @Test
    public void resolveException_returns_null_if_maybeHandleException_throws_UnexpectedMajorExceptionHandlingError()
        throws UnexpectedMajorExceptionHandlingError {
        // given
        doThrow(new UnexpectedMajorExceptionHandlingError("foo", null))
            .when(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));

        // when
        ModelAndView result = handlerSpy.resolveException(null, null, null, null);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(result).isNull();
    }

    @Test
    public void order_getters_and_setters_work() {
        // given
        int newOrder = handlerSpy.getOrder() + 42;

        // when
        handlerSpy.setOrder(newOrder);

        // then
        assertThat(handlerSpy.getOrder()).isEqualTo(newOrder);
    }

    @Test
    public void default_order_is_highest_precedence_plus_one() {
        // given
        SpringApiExceptionHandler impl = new SpringApiExceptionHandler(
            projectApiErrorsMock, listenerList, generalUtils, springUtils
        );

        // when
        int defaultOrder = impl.getOrder();

        // then
        assertThat(defaultOrder).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

}