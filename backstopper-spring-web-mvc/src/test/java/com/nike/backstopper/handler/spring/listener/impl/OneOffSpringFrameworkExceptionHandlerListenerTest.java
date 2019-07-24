package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link OneOffSpringFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringFrameworkExceptionHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private OneOffSpringFrameworkExceptionHandlerListener listener = new OneOffSpringFrameworkExceptionHandlerListener(testProjectApiErrors,
                                                                                                                       ApiExceptionHandlerUtils.DEFAULT_IMPL);

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringFrameworkExceptionHandlerListener impl = new OneOffSpringFrameworkExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringFrameworkExceptionHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringFrameworkExceptionHandlerListener(mock(ProjectApiErrors.class), null)
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

    @Test
    public void shouldHandleException_returns_ignoreResponse_if_passed_exception_it_does_not_want_to_handle() {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(
            new ApiException(testProjectApiErrors.getGenericServiceError())
        );

        // then
        validateResponse(result, false, null);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldReturnTYPE_CONVERSION_ERRORForTypeMismatchException(
        boolean isMethodArgTypeMismatchEx
    ) {
        // given
        Object valueObj = "notAnInteger";
        Class<?> requiredType = Integer.class;
        Throwable someCause = new RuntimeException("some cause");
        String methodArgName = "fooArg";
        MethodParameter methodParam = mock(MethodParameter.class);

        TypeMismatchException ex =
            (isMethodArgTypeMismatchEx)
            ? new MethodArgumentTypeMismatchException(valueObj, requiredType, methodArgName, methodParam, someCause)
            : new TypeMismatchException(valueObj, requiredType, someCause);

        String expectedBadPropName = (isMethodArgTypeMismatchEx) ? methodArgName : null;

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>(
            Arrays.asList(
                Pair.of("exception_message", ex.getMessage()),
                Pair.of("bad_property_name", expectedBadPropName),
                Pair.of("bad_property_value", String.valueOf(valueObj)),
                Pair.of("required_type", String.valueOf(requiredType))
            )
        );

        if (isMethodArgTypeMismatchEx) {
            expectedExtraDetailsForLogging.add(Pair.of("method_arg_name", methodArgName));
            expectedExtraDetailsForLogging.add(Pair.of("method_arg_target_param", methodParam.toString()));
        }

        Map<String, Object> expectedMetadata = MapBuilder
            .builder("bad_property_value", valueObj)
            .put("required_type", "int")
            .build();

        if (expectedBadPropName != null) {
            expectedMetadata.put("bad_property_name", expectedBadPropName);
        }

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(
            new ApiErrorWithMetadata(testProjectApiErrors.getTypeConversionApiError(), expectedMetadata)
        ));
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrderElementsOf(expectedExtraDetailsForLogging);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_returns_GENERIC_SERVICE_ERROR_for_ConversionNotSupportedException(
        boolean isMethodArgConversionEx
    ) {
        // given
        Object valueObj = "notAnInteger";
        Class<?> requiredType = Integer.class;
        Throwable someCause = new RuntimeException("some cause");
        String methodArgName = "fooArg";
        MethodParameter methodParam = mock(MethodParameter.class);

        ConversionNotSupportedException ex =
            (isMethodArgConversionEx)
            ? new MethodArgumentConversionNotSupportedException(valueObj, requiredType, methodArgName, methodParam, someCause)
            : new ConversionNotSupportedException(valueObj, requiredType, someCause);

        String expectedBadPropName = (isMethodArgConversionEx) ? methodArgName : null;

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>(
            Arrays.asList(
                Pair.of("exception_message", ex.getMessage()),
                Pair.of("bad_property_name", expectedBadPropName),
                Pair.of("bad_property_value", String.valueOf(valueObj)),
                Pair.of("required_type", String.valueOf(requiredType))
            )
        );

        if (isMethodArgConversionEx) {
            expectedExtraDetailsForLogging.add(Pair.of("method_arg_name", methodArgName));
            expectedExtraDetailsForLogging.add(Pair.of("method_arg_target_param", methodParam.toString()));
        }

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getGenericServiceError()));
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrderElementsOf(expectedExtraDetailsForLogging);
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
            ? String.format("Required %s parameter '%s' is not present", missingParamType, missingParamName)
            : "foo";

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(expectedResult));
        assertThat(result.extraDetailsForLogging)
            .containsExactly(Pair.of("exception_message", expectedExceptionMessage));
    }

    @Test
    public void shouldHandleException_returns_MALFORMED_REQUEST_for_generic_HttpMessageConversionException() {
        // given
        HttpMessageConversionException ex = new HttpMessageConversionException("foobar");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getMalformedRequestApiError()));
        assertThat(result.extraDetailsForLogging).containsExactly(Pair.of("exception_message", ex.getMessage()));
    }

    private enum HttpMessageNotReadableExceptionScenario {
        KNOWN_MESSAGE_FOR_MISSING_CONTENT(
            new HttpMessageNotReadableException("Required request body is missing " + UUID.randomUUID().toString()),
            ProjectApiErrors::getMissingExpectedContentApiError
        ),
        JSON_MAPPING_EXCEPTION_CAUSE_INDICATING_NO_CONTENT(
            new HttpMessageNotReadableException("foobar", new JsonMappingException("No content to map due to end-of-input")),
            ProjectApiErrors::getMissingExpectedContentApiError
        ),
        CAUSE_IS_NULL(
            new HttpMessageNotReadableException("foobar", null),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_NOT_JSON_MAPPING_EXCEPTION(
            new HttpMessageNotReadableException("foobar", new Exception("No content to map due to end-of-input")),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_JSON_MAPPING_EXCEPTION_BUT_JME_MESSAGE_IS_NULL(
            new HttpMessageNotReadableException("foobar", mock(JsonMappingException.class)),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_JSON_MAPPING_EXCEPTION_BUT_JME_MESSAGE_IS_NOT_THE_NO_CONTENT_MESSAGE(
            new HttpMessageNotReadableException("foobar", new JsonMappingException("garbagio")),
            ProjectApiErrors::getMalformedRequestApiError
        );

        public final HttpMessageNotReadableException ex;
        private final Function<ProjectApiErrors, ApiError> expectedErrorExtractor;

        HttpMessageNotReadableExceptionScenario(
            HttpMessageNotReadableException ex,
            Function<ProjectApiErrors, ApiError> expectedErrorExtractor
        ) {
            this.ex = ex;
            this.expectedErrorExtractor = expectedErrorExtractor;
        }

        public ApiError getExpectedError(ProjectApiErrors pae) {
            return expectedErrorExtractor.apply(pae);
        }
    }

    @DataProvider
    public static List<List<HttpMessageNotReadableExceptionScenario>> httpMessageNotReadableExceptionScenarioDataProvider() {
        return Stream.of(HttpMessageNotReadableExceptionScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("httpMessageNotReadableExceptionScenarioDataProvider")
    @Test
    public void shouldHandleException_returns_expected_error_for_HttpMessageNotReadableException(
        HttpMessageNotReadableExceptionScenario scenario
    ) {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(scenario.ex);

        // then
        validateResponse(result, true, singletonList(scenario.getExpectedError(testProjectApiErrors)));
        assertThat(result.extraDetailsForLogging).containsExactly(Pair.of("exception_message", scenario.ex.getMessage()));
    }

    @Test
    public void shouldHandleException_returns_GENERIC_SERVICE_ERROR_for_HttpMessageNotWritableException() {
        // given
        HttpMessageNotWritableException ex = new HttpMessageNotWritableException("foobar");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getGenericServiceError()));
        assertThat(result.extraDetailsForLogging).containsExactly(Pair.of("exception_message", ex.getMessage()));
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

    @Test
    public void shouldHandleException_returns_METHOD_NOT_ALLOWED_for_HttpRequestMethodNotSupportedException() {
        // given
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("asplode");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getMethodNotAllowedApiError()));
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
    public void shouldHandleException_returns_TEMPORARY_SERVICE_PROBLEM_for_AsyncRequestTimeoutException() {
        // given
        AsyncRequestTimeoutException ex = new AsyncRequestTimeoutException();
        assertThat(ex.getClass().getName())
            .isEqualTo("org.springframework.web.context.request.async.AsyncRequestTimeoutException");

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getTemporaryServiceProblemApiError()));
    }

    @Test
    public void shouldHandleException_should_return_not_found_error_when_passed_NoHandlerFoundException() {
        // given
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/some/url", mock(HttpHeaders.class));

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @Test
    public void shouldHandleException_should_return_not_found_error_when_passed_NoSuchRequestHandlingMethodException() {
        // given
        NoSuchRequestHandlingMethodException ex = new NoSuchRequestHandlingMethodException(
            "/some/url", "GET", new HashMap<>()
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @DataProvider
    public static Object[][] dataProviderForExtractRequiredTypeNoInfoLeak() {
        return new Object[][] {
            { null, null },
            { Byte.class, "byte" },
            { byte.class, "byte" },
            { Short.class, "short" },
            { short.class, "short" },
            { Integer.class, "int" },
            { int.class, "int" },
            { Long.class, "long" },
            { long.class, "long" },
            { Float.class, "float" },
            { float.class, "float" },
            { Double.class, "double" },
            { double.class, "double" },
            { Boolean.class, "boolean" },
            { boolean.class, "boolean" },
            { Character.class, "char" },
            { char.class, "char" },
            { CharSequence.class, "string" },
            { Object.class, "[complex type]"}
        };
    }

    @UseDataProvider("dataProviderForExtractRequiredTypeNoInfoLeak")
    @Test
    public void extractRequiredTypeNoInfoLeak_works_as_expected(Class<?> requiredType, String expectedResult) {
        // given
        TypeMismatchException typeMismatchExceptionMock = mock(TypeMismatchException.class);
        doReturn(requiredType).when(typeMismatchExceptionMock).getRequiredType();

        // when
        String result = listener.extractRequiredTypeNoInfoLeak(typeMismatchExceptionMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void extractPropertyName_works_for_MethodArgumentTypeMismatchException() {
        // given
        MethodArgumentTypeMismatchException exMock = mock(MethodArgumentTypeMismatchException.class);
        String name = UUID.randomUUID().toString();
        doReturn(name).when(exMock).getName();

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void extractPropertyName_works_for_MethodArgumentConversionNotSupportedException() {
        // given
        MethodArgumentConversionNotSupportedException exMock = mock(MethodArgumentConversionNotSupportedException.class);
        String name = UUID.randomUUID().toString();
        doReturn(name).when(exMock).getName();

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void extractPropertyName_returns_null_for_base_TypeMismatchException() {
        // given
        TypeMismatchException exMock = mock(TypeMismatchException.class);

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        assertThat(result).isNull();
    }
}
