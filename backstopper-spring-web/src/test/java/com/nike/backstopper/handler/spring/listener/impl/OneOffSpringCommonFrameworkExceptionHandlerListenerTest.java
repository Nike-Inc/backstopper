package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.SortedApiErrorSet;
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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link OneOffSpringCommonFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringCommonFrameworkExceptionHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private OneOffSpringCommonFrameworkExceptionHandlerListener listener = new OneOffListenerBasicImpl(
        testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL
    );

    private static class OneOffListenerBasicImpl extends OneOffSpringCommonFrameworkExceptionHandlerListener {

        public OneOffListenerBasicImpl(
            ProjectApiErrors projectApiErrors,
            ApiExceptionHandlerUtils utils
        ) {
            super(projectApiErrors, utils);
        }

        @Override
        protected @NotNull ApiExceptionHandlerListenerResult handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
            @NotNull Throwable ex
        ) {
            return ApiExceptionHandlerListenerResult.ignoreResponse();
        }
    }

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringCommonFrameworkExceptionHandlerListener
            impl = new OneOffListenerBasicImpl(projectErrorsMock, utilsMock);

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffListenerBasicImpl(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffListenerBasicImpl(mock(ProjectApiErrors.class), null)
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

    @Test
    public void shouldHandleException_defers_to_handleSpringMvcOrWebfluxSpecificFrameworkExceptions_if_that_method_wants_to_handle_exception() {
        // given
        OneOffSpringCommonFrameworkExceptionHandlerListener listenerSpy = spy(listener);
        Throwable ex = mock(Throwable.class);
        ApiExceptionHandlerListenerResult expectedResult = ApiExceptionHandlerListenerResult.handleResponse(
            SortedApiErrorSet.singletonSortedSetOf(mock(ApiError.class))
        );
        doReturn(expectedResult).when(listenerSpy).handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // when
        ApiExceptionHandlerListenerResult result = listenerSpy.shouldHandleException(ex);

        // then
        assertThat(result).isSameAs(expectedResult);
        verify(listenerSpy).handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);
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
    public void handleTypeMismatchException_adds_exception_message_logging_detail_depending_on_method_arg(
        boolean shouldAddExceptionMsg
    ) {
        // given
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();

        // when
        ApiExceptionHandlerListenerResult result = listener.handleTypeMismatchException(
            new TypeMismatchException("doesNotMatter", Integer.class), extraDetailsForLogging, shouldAddExceptionMsg
        );

        // then
        assertThat(result.extraDetailsForLogging).isEqualTo(extraDetailsForLogging);
        Optional<Pair<String, String>> exMsgPairOpt = extraDetailsForLogging
            .stream()
            .filter(p -> p.getKey().equals("exception_message"))
            .findAny();

        if (shouldAddExceptionMsg) {
            assertThat(exMsgPairOpt).isPresent();
        }
        else {
            assertThat(exMsgPairOpt).isEmpty();
        }
    }

    @DataProvider(value = {
        "false  |   false   |   false",
        "true   |   false   |   false",
        "false  |   true    |   false",
        "false  |   false   |   true",
        "true   |   true    |   true",
    }, splitBy = "\\|")
    @Test
    public void handleTypeMismatchException_adds_metadata_to_resulting_ApiError_as_expected(
        boolean propNameExists, boolean propValueExists, boolean requiredTypeExists
    ) {
        // given
        TypeMismatchException exMock = mock(TypeMismatchException.class);
        String propName = (propNameExists) ? UUID.randomUUID().toString() : null;
        String propValue = (propValueExists) ? UUID.randomUUID().toString() : null;
        Class<?> requiredType = (requiredTypeExists) ? Integer.class : null;
        String expectedRequiredType = (requiredTypeExists) ? "int" : null;

        doReturn(propName).when(exMock).getPropertyName();
        doReturn(propValue).when(exMock).getValue();
        doReturn(requiredType).when(exMock).getRequiredType();

        // when
        ApiExceptionHandlerListenerResult result = listener.handleTypeMismatchException(
            exMock, new ArrayList<>(), true
        );

        // then
        assertThat(result.errors).hasSize(1);
        ApiError apiError = result.errors.iterator().next();
        Object propNameMetadata = apiError.getMetadata().get("bad_property_name");
        Object propValueMetadata = apiError.getMetadata().get("bad_property_value");
        Object requiredTypeMetadata = apiError.getMetadata().get("required_type");
        assertThat(propNameMetadata).isEqualTo(propName);
        assertThat(propValueMetadata).isEqualTo(propValue);
        assertThat(requiredTypeMetadata).isEqualTo(expectedRequiredType);
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
            new HttpMessageNotReadableException("foobar", null, null),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_NOT_JSON_MAPPING_EXCEPTION(
            new HttpMessageNotReadableException("foobar", new Exception("No content to map due to end-of-input")),
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
        NoHandlerFoundException ex = new NoHandlerFoundException();

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @Test
    public void shouldHandleException_should_return_not_found_error_when_passed_NoSuchRequestHandlingMethodException() {
        // given
        NoSuchRequestHandlingMethodException ex = new NoSuchRequestHandlingMethodException();

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @DataProvider
    public static List<List<Throwable>> unauthorized401ExceptionsDataProvider() {
        return Stream.<Throwable>of(
            new BadCredentialsException("foo"),
            new InsufficientAuthenticationException("foo"),
            new AuthenticationCredentialsNotFoundException("foo"),
            new LockedException("foo"),
            new DisabledException("foo"),
            new CredentialsExpiredException("foo"),
            new AccountExpiredException("foo"),
            new UsernameNotFoundException("foo"),
            new RemoteAuthenticationException("foo")
        ).map(Collections::singletonList)
         .collect(Collectors.toList());
    }

    @UseDataProvider("unauthorized401ExceptionsDataProvider")
    @Test
    public void shouldHandleException_returns_UNAUTHORIZED_for_exceptions_that_map_to_401(
        Throwable ex
    ) {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getUnauthorizedApiError()));
    }

    @DataProvider
    public static List<List<Throwable>> forbidden403ExceptionsDataProvider() {
        return singletonList(
            singletonList(new AccessDeniedException("foo"))
        );
    }

    @UseDataProvider("forbidden403ExceptionsDataProvider")
    @Test
    public void shouldHandleException_returns_FORBIDDEN_for_exceptions_that_map_to_403(
        Throwable ex
    ) {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, singletonList(testProjectApiErrors.getForbiddenApiError()));
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

    @DataProvider(value = {
        "some foo bar string    |   foo bar |   true",
        "some foo bar string    |   nope    |   false",
        "null                   |   blah    |   false",
        "blah                   |   null    |   false",
        "null                   |   null    |   false",
    }, splitBy = "\\|")
    @Test
    public void nullSafeStringContains_works_as_expected(
        String strToCheck, String snippet, boolean expectedResult
    ) {
        // when
        boolean result = listener.nullSafeStringContains(strToCheck, snippet);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }
}
