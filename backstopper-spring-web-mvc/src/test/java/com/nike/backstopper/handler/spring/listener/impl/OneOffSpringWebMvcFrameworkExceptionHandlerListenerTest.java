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
import org.springframework.core.MethodParameter;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singleton;
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
    
    private final OneOffSpringWebMvcFrameworkExceptionHandlerListener listener =
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

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle,
        Collection<? extends ApiError> expectedErrors,
        List<Pair<String, String>> expectedExtraDetailsForLogging
    ) {
        validateResponse(result, expectedShouldHandle, expectedErrors);
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrderElementsOf(expectedExtraDetailsForLogging);
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
        String missingParamName = "someParam-" + UUID.randomUUID();
        String missingParamType = "someParamType-" + UUID.randomUUID();
        ServletRequestBindingException ex =
            (isMissingRequestParamEx)
            ? new MissingServletRequestParameterException(missingParamName, missingParamType)
            : new ServletRequestBindingException("foo");

        ApiError expectedResult = testProjectApiErrors.getMalformedRequestApiError();
        if (isMissingRequestParamEx) {
            expectedResult = new ApiErrorWithMetadata(
                expectedResult,
                Pair.of("missing_param_name", missingParamName),
                Pair.of("missing_param_type", missingParamType),
                Pair.of("required_location", "query_param")
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

    public void methodWithAnnotatedParams(
        @RequestHeader int headerParam,
        @RequestParam int queryParam,
        @RequestHeader @RequestParam int bothParam,
        int unknownParam
    ) {
        // This method is used as part of shouldHandleException_handles_MissingRequestHeaderException_as_expected().
    }

    @Test
    public void shouldHandleException_handles_MissingRequestHeaderException_as_expected() throws NoSuchMethodException {
        // given
        Method method = this.getClass()
                            .getDeclaredMethod("methodWithAnnotatedParams", int.class, int.class, int.class, int.class);
        MethodParameter headerParamDetails = new MethodParameter(method, 0);

        String missingHeaderName = "some-header-" + UUID.randomUUID();
        MissingRequestHeaderException ex = new MissingRequestHeaderException(missingHeaderName, headerParamDetails);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        Map<String, Object> expectedMetadata = new LinkedHashMap<>();
        expectedMetadata.put("missing_param_name", missingHeaderName);
        expectedMetadata.put("missing_param_type", "int");
        expectedMetadata.put("required_location", "header");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(new ApiErrorWithMetadata(
                testProjectApiErrors.getMalformedRequestApiError(),
                expectedMetadata
            )),
            expectedExtraDetailsForLogging
        );
    }
}