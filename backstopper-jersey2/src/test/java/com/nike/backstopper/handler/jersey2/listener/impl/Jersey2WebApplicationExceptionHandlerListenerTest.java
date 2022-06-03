package com.nike.backstopper.handler.jersey2.listener.impl;

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
import org.glassfish.jersey.server.ParamException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.WebApplicationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link Jersey2WebApplicationExceptionHandlerListener}
 *
 * Created by dsand7 on 9/25/14.
 */
@RunWith(DataProviderRunner.class)
public class Jersey2WebApplicationExceptionHandlerListenerTest extends ListenerTestBase {

    private static Jersey2WebApplicationExceptionHandlerListener listener;
    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private static final ApiExceptionHandlerUtils utils = ApiExceptionHandlerUtils.DEFAULT_IMPL;

    @BeforeClass
    public static void setupClass() {
        listener = new Jersey2WebApplicationExceptionHandlerListener(testProjectApiErrors, utils);
    }

    @Test
    public void constructor_sets_fields_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        Jersey2WebApplicationExceptionHandlerListener
            impl = new Jersey2WebApplicationExceptionHandlerListener(projectErrorsMock, utilsMock);

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
                new Jersey2WebApplicationExceptionHandlerListener(null, utils);
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
                new Jersey2WebApplicationExceptionHandlerListener(testProjectApiErrors, null);
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
    public void shouldIgnoreWebApplicationExceptionThatItDoesNotWantToHandle() {

        WebApplicationException exception = new WebApplicationException();

        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(exception);

        validateResponse(result, false, null);
    }

    @DataProvider
    public static Object[][] dataProviderForShouldHandleException() {
        return new Object[][] {
            { mock(ParamException.UriParamException.class), testProjectApiErrors.getNotFoundApiError() },
            { mock(ParamException.class), testProjectApiErrors.getMalformedRequestApiError() }
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
