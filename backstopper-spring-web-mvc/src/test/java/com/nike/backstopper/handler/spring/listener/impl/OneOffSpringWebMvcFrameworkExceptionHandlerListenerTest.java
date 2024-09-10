package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link OneOffSpringWebMvcFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringWebMvcFrameworkExceptionHandlerListenerTest {
    
    private static final ProjectApiErrors testProjectApiErrors =
        ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    
    private OneOffSpringWebMvcFrameworkExceptionHandlerListener listener =
        new OneOffSpringWebMvcFrameworkExceptionHandlerListener(
            testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL
        );

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringWebMvcFrameworkExceptionHandlerListener
            impl = new OneOffSpringWebMvcFrameworkExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebMvcFrameworkExceptionHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebMvcFrameworkExceptionHandlerListener(mock(ProjectApiErrors.class), null)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldHandleException_returns_ignoreResponse_if_passed_null_exception() {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(null);

        // then
        validateResponse(result, false, null);
    }

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle, 
        Collection<? extends ApiError> expectedErrors
    ) {
        if (!expectedShouldHandle) {
            assertThat(result.shouldHandleResponse).isFalse();
            return;
        }

        assertThat(result.errors).containsExactlyInAnyOrderElementsOf(expectedErrors);
    }

    @Test
    public void shouldHandleException_returns_ignoreResponse_if_passed_exception_it_does_not_want_to_handle() {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(
            new ApiException(testProjectApiErrors.getGenericServiceError())
        );

        // then
        validateResponse(result, false, null);
    }

    @Test
    public void shouldHandleException_returns_METHOD_NOT_ALLOWED_for_HttpRequestMethodNotSupportedException() {
        // given
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("asplode");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getMethodNotAllowedApiError()));
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_returns_MALFORMED_REQUEST_for_ServletRequestBindingException(
        boolean isMissingRequestParamEx
    ) {
        // given
        String missingParamName = "someParam-" + UUID.randomUUID().toString();
        String missingParamType = "someParamType-" + UUID.randomUUID().toString();
        ServletRequestBindingException ex =
            (isMissingRequestParamEx)
            ? new MissingServletRequestParameterException(missingParamName, missingParamType)
            : new ServletRequestBindingException("foo");

        ApiError expectedResult = testProjectApiErrors.getMalformedRequestApiError();
        if (isMissingRequestParamEx) {
            expectedResult = new ApiErrorWithMetadata(
                expectedResult,
                Pair.of("missing_param_name", missingParamName),
                Pair.of("missing_param_type", missingParamType)
            );
        }

        String expectedExceptionMessage =
            (isMissingRequestParamEx)
            ? String.format("Required request parameter '%s' for method parameter type %s is not present", missingParamName, missingParamType)
            : "foo";

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(expectedResult));
        assertThat(result.extraDetailsForLogging)
            .containsExactly(Pair.of("exception_message", expectedExceptionMessage));
    }

    @Test
    public void shouldHandleException_returns_MALFORMED_REQUEST_for_MissingServletRequestPartException() {
        // given
        String partName = UUID.randomUUID().toString();
        MissingServletRequestPartException ex = new MissingServletRequestPartException(partName);

        ApiError expectedResult = new ApiErrorWithMetadata(
            testProjectApiErrors.getMalformedRequestApiError(),
            Pair.of("missing_required_part", partName)
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(expectedResult));
    }

    @Test
    public void shouldHandleException_returns_NO_ACCEPTABLE_REPRESENTATION_for_HttpMediaTypeNotAcceptableException() {
        // given
        HttpMediaTypeNotAcceptableException ex = new HttpMediaTypeNotAcceptableException("asplode");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getNoAcceptableRepresentationApiError()));
    }

    @Test
    public void shouldHandleException_returns_UNSUPPORTED_MEDIA_TYPE_for_HttpMediaTypeNotSupportedException() {
        // given
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("asplode");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getUnsupportedMediaTypeApiError()));
    }
}