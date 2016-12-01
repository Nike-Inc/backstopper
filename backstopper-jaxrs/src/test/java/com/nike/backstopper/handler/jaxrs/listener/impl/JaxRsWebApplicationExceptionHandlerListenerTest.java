package com.nike.backstopper.handler.jaxrs.listener.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link JaxRsWebApplicationExceptionHandlerListener}
 *
 * @author dsand7
 * @author Michael Irwin
 */
@RunWith(DataProviderRunner.class)
public class JaxRsWebApplicationExceptionHandlerListenerTest extends ListenerTestBase {

    private static JaxRsWebApplicationExceptionHandlerListener listener;
    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private static final ApiExceptionHandlerUtils utils = ApiExceptionHandlerUtils.DEFAULT_IMPL;

    @BeforeClass
    public static void setupClass() {
        listener = new JaxRsWebApplicationExceptionHandlerListener(testProjectApiErrors, utils);
    }

    @Test
    public void constructor_sets_fields_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        JaxRsWebApplicationExceptionHandlerListener
            impl = new JaxRsWebApplicationExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new JaxRsWebApplicationExceptionHandlerListener(null, utils);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new JaxRsWebApplicationExceptionHandlerListener(testProjectApiErrors, null);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new ApiException(testProjectApiErrors.getGenericServiceError())), false, null);
    }

    @Test
    public void shouldCreateValidationErrorsForWebApplicationException() {

        NotFoundException exception = new NotFoundException("/fake/uri");

        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(exception);

        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @Test
    public void shouldIgnoreWebApplicationExceptionThatItDoesNotWantToHandle() {

        WebApplicationException exception = new WebApplicationException();

        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(exception);

        validateResponse(result, false, null);
    }

    @DataProvider
    public static Object[][] dataProviderForShouldHandleException() {
        return new Object[][] {
            { new NotFoundException(), testProjectApiErrors.getNotFoundApiError() },
            { new WebApplicationException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), testProjectApiErrors.getUnsupportedMediaTypeApiError() },
            { new WebApplicationException(HttpServletResponse.SC_METHOD_NOT_ALLOWED), testProjectApiErrors.getMethodNotAllowedApiError() },
            { new WebApplicationException(HttpServletResponse.SC_UNAUTHORIZED), testProjectApiErrors.getUnauthorizedApiError() },
            { new WebApplicationException(HttpServletResponse.SC_NOT_ACCEPTABLE), testProjectApiErrors.getNoAcceptableRepresentationApiError() },
            { mock(JsonProcessingException.class), testProjectApiErrors.getMalformedRequestApiError() }
        };
    }

    @UseDataProvider("dataProviderForShouldHandleException")
    @Test
    public void shouldHandleException_handles_exceptions_it_knows_about(Exception ex, ApiError expectedResultError) {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        assertThat(result.shouldHandleResponse).isTrue();
        assertThat(result.errors).isEqualTo(SortedApiErrorSet.singletonSortedSetOf(expectedResultError));
    }
}
