package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
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
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
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
@SuppressWarnings("ClassEscapesDefinedScope")
public class OneOffSpringCommonFrameworkExceptionHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private final OneOffSpringCommonFrameworkExceptionHandlerListener listener = new OneOffListenerBasicImpl(
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
        @SuppressWarnings("DataFlowIssue")
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffListenerBasicImpl(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        @SuppressWarnings("DataFlowIssue")
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
            new TypeMismatchException("doesNotMatter", Integer.class), extraDetailsForLogging, shouldAddExceptionMsg, null
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
            exMock, new ArrayList<>(), true, null
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

    @Test
    public void handleTypeMismatchException_adds_extra_metadata_to_resulting_ApiError_if_specified() {
        // given
        TypeMismatchException exMock = mock(TypeMismatchException.class);
        String propName = UUID.randomUUID().toString();
        String propValue = UUID.randomUUID().toString();
        Class<?> requiredType = Integer.class;
        String expectedRequiredType = "int";

        doReturn(propName).when(exMock).getPropertyName();
        doReturn(propValue).when(exMock).getValue();
        doReturn(requiredType).when(exMock).getRequiredType();

        List<Pair<String, String>> extraMetadata = Arrays.asList(
            Pair.of("foo_extra_metadata", UUID.randomUUID().toString()),
            Pair.of("bar_extra_metadata", UUID.randomUUID().toString())
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleTypeMismatchException(
            exMock, new ArrayList<>(), true, extraMetadata
        );

        // then
        assertThat(result.errors).hasSize(1);
        ApiError apiError = result.errors.iterator().next();
        Object propNameMetadata = apiError.getMetadata().get("bad_property_name");
        Object propValueMetadata = apiError.getMetadata().get("bad_property_value");
        Object requiredTypeMetadata = apiError.getMetadata().get("required_type");
        Object fooExtraMetadata = apiError.getMetadata().get("foo_extra_metadata");
        Object barExtraMetadata = apiError.getMetadata().get("bar_extra_metadata");
        assertThat(propNameMetadata).isEqualTo(propName);
        assertThat(propValueMetadata).isEqualTo(propValue);
        assertThat(requiredTypeMetadata).isEqualTo(expectedRequiredType);
        assertThat(fooExtraMetadata).isEqualTo(extraMetadata.get(0).getRight());
        assertThat(barExtraMetadata).isEqualTo(extraMetadata.get(1).getRight());
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
            new HttpMessageNotReadableException("Required request body is missing " + UUID.randomUUID(),
                                                mock(HttpInputMessage.class)),
            ProjectApiErrors::getMissingExpectedContentApiError
        ),
        JSON_MAPPING_EXCEPTION_CAUSE_INDICATING_NO_CONTENT(
            new HttpMessageNotReadableException("foobar", new JsonMappingException(null, "No content to map due to end-of-input"),
                                                mock(HttpInputMessage.class)),
            ProjectApiErrors::getMissingExpectedContentApiError
        ),
        CAUSE_IS_NULL(
            new HttpMessageNotReadableException("foobar", null, mock(HttpInputMessage.class)),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_NOT_JSON_MAPPING_EXCEPTION(
            new HttpMessageNotReadableException("foobar", new Exception("No content to map due to end-of-input"),
                                                mock(HttpInputMessage.class)),
            ProjectApiErrors::getMalformedRequestApiError
        ),
        CAUSE_IS_JSON_MAPPING_EXCEPTION_BUT_JME_MESSAGE_IS_NOT_THE_NO_CONTENT_MESSAGE(
            new HttpMessageNotReadableException("foobar", new JsonMappingException(null, "garbagio"),
                                                mock(HttpInputMessage.class)),
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
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/foo", new HttpHeaders());

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
            new UsernameNotFoundException("foo")
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
        // when
        String result = listener.extractRequiredTypeNoInfoLeak(requiredType);

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

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        @SuppressWarnings("SameParameterValue") boolean expectedShouldHandle,
        Collection<? extends ApiError> expectedErrors,
        List<Pair<String, String>> expectedExtraDetailsForLogging
    ) {
        if (!expectedShouldHandle) {
            assertThat(result.shouldHandleResponse).isFalse();
            return;
        }

        assertThat(result.errors).containsExactlyInAnyOrderElementsOf(expectedErrors);
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrderElementsOf(expectedExtraDetailsForLogging);
    }

    private enum TypeMismatchExceptionScenario {
        CONVERSION_NOT_SUPPORTED_500(
            HttpStatus.valueOf(500),
            new ConversionNotSupportedException(
                new PropertyChangeEvent("doesNotMatter", "somePropertyName", "oldValue", "newValue"),
                Integer.class,
                null
            ),
            testProjectApiErrors.getGenericServiceError(),
            Arrays.asList(
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", Integer.class.toString())
            )
        ),
        GENERIC_TYPE_MISMATCH_EXCEPTION_400(
            HttpStatus.valueOf(400),
            new TypeMismatchException(
                new PropertyChangeEvent("doesNotMatter", "somePropertyName", "oldValue", "newValue"),
                Integer.class
            ),
            new ApiErrorWithMetadata(
                testProjectApiErrors.getTypeConversionApiError(),
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", "int")
            ),
            Arrays.asList(
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", Integer.class.toString())
            )
        ),
        UNEXPECTED_4XX_STATUS_CODE(
            HttpStatus.valueOf(403),
            new TypeMismatchException("doesNotMatter", Integer.class),
            testProjectApiErrors.getForbiddenApiError(),
            Collections.emptyList()
        ),
        UNEXPECTED_5XX_STATUS_CODE(
            HttpStatus.valueOf(503),
            new TypeMismatchException("doesNotMatter", Integer.class),
            testProjectApiErrors.getTemporaryServiceProblemApiError(),
            Collections.emptyList()
        ),
        UNKNOWN_4XX_STATUS_CODE(
            HttpStatus.valueOf(418),
            new TypeMismatchException("doesNotMatter", Integer.class),
            new ApiErrorBase(
                "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_418",
                testProjectApiErrors.getGenericBadRequestApiError().getErrorCode(),
                "An error occurred that resulted in response status code 418",
                418
            ),
            Collections.emptyList()
        ),
        UNKNOWN_5XX_STATUS_CODE(
            HttpStatus.valueOf(509),
            new TypeMismatchException("doesNotMatter", Integer.class),
            new ApiErrorBase(
                "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_509",
                testProjectApiErrors.getGenericServiceError().getErrorCode(),
                "An error occurred that resulted in response status code 509",
                509
            ),
            Collections.emptyList()
        );

        public final HttpStatus status;
        public final TypeMismatchException tmeCause;
        public final ApiError expectedApiError;
        public final List<Pair<String, String>> expectedExtraDetailsForLogging;

        TypeMismatchExceptionScenario(
            HttpStatus status, TypeMismatchException tmeCause, ApiError expectedApiError,
            List<Pair<String, String>> expectedExtraDetailsForLogging
        ) {
            this.status = status;
            this.tmeCause = tmeCause;
            this.expectedApiError = expectedApiError;
            this.expectedExtraDetailsForLogging = expectedExtraDetailsForLogging;
        }
    }

    @DataProvider
    public static List<List<TypeMismatchExceptionScenario>> typeMismatchExceptionScenarioDataProvider() {
        return Stream.of(TypeMismatchExceptionScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("typeMismatchExceptionScenarioDataProvider")
    @Test
    public void shouldHandleException_handles_ResponseStatusException_with_TypeMismatchException_cause_as_expected(
        TypeMismatchExceptionScenario scenario
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            scenario.status, "Some ResponseStatusException reason", scenario.tmeCause
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );
        expectedExtraDetailsForLogging.addAll(scenario.expectedExtraDetailsForLogging);

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(scenario.expectedApiError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   MALFORMED_REQUEST",
        "401    |   UNAUTHORIZED"
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_returns_MALFORMED_REQUEST_for_ResponseStatusException_with_DecodingException_cause_only_if_status_is_400(
        int statusCode, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.valueOf(statusCode),
            "Some ResponseStatusException reason",
            new DecodingException("Some decoding ex")
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "No request body    |   ",
        "                   |   No request body for: blah"
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_returns_MISSING_EXPECTED_CONTENT_for_ResponseStatusException_with_DecodingException_cause_with_magic_messages(
        String exReason, String decodingExMessage
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.valueOf(400),
            exReason,
            new DecodingException(decodingExMessage)
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   Required foo parameter 'bar' is not present |   foo     |   bar     |   MALFORMED_REQUEST",
        "401    |   Required foo parameter 'bar' is not present |   null    |   null    |   UNAUTHORIZED",
        "400    |   Required parameter 'bar' is not present     |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo parameter is not present       |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Blah foo parameter 'bar' is not present     |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo blah 'bar' is not present      |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo parameter 'bar' is not blah    |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Some random reason                          |   null    |   null    |   GENERIC_BAD_REQUEST",
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_returns_MALFORMED_REQUEST_for_ResponseStatusException_with_special_required_param_reason_string(
        int statusCode,
        String exReasonString,
        String expectedMissingParamType,
        String expectedMissingParamName,
        BarebonesCoreApiErrorForTesting expectedBaseError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.valueOf(statusCode), exReasonString);
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        ApiError expectedError = expectedBaseError;
        if (expectedMissingParamName != null && expectedMissingParamType != null) {
            expectedError = new ApiErrorWithMetadata(
                expectedBaseError,
                Pair.of("missing_param_name", expectedMissingParamName),
                Pair.of("missing_param_type", expectedMissingParamType)
            );
        }

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   Request body is missing             |   MISSING_EXPECTED_CONTENT",
        "400    |   Request body is missing blahblah    |   MISSING_EXPECTED_CONTENT",
        "401    |   Request body is missing             |   UNAUTHORIZED",
        "400    |   Request body is                     |   GENERIC_BAD_REQUEST",
        "400    |   Some random reason                  |   GENERIC_BAD_REQUEST",
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_returns_MISSING_EXPECTED_CONTENT_for_ResponseStatusException_with_special_reason_string_beginning(
        int statusCode, String exReasonString, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.valueOf(statusCode), exReasonString);
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   GENERIC_BAD_REQUEST",
        "401    |   UNAUTHORIZED",
        "403    |   FORBIDDEN",
        "404    |   NOT_FOUND",
        "405    |   METHOD_NOT_ALLOWED",
        "406    |   NO_ACCEPTABLE_REPRESENTATION",
        "415    |   UNSUPPORTED_MEDIA_TYPE",
        "429    |   TOO_MANY_REQUESTS",
        "500    |   GENERIC_SERVICE_ERROR",
        "503    |   TEMPORARY_SERVICE_PROBLEM",
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_handles_generic_ResponseStatusException_by_returning_ApiError_from_project_if_status_code_is_known(
        int desiredStatusCode, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.valueOf(desiredStatusCode), "Some ResponseStatusException reason"
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "418",
        "509"
    })
    @Test
    @SuppressWarnings("ExtractMethodRecommender")
    public void shouldHandleException_handles_generic_ResponseStatusException_by_returning_synthetic_ApiError_if_status_code_is_unknown(
        int desiredStatusCode
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.valueOf(desiredStatusCode), "Some ResponseStatusException reason"
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedErrorCode = (desiredStatusCode >= 500)
                                   ? testProjectApiErrors.getGenericServiceError().getErrorCode()
                                   : testProjectApiErrors.getGenericBadRequestApiError().getErrorCode();

        ApiError expectedError = new ApiErrorBase(
            "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_" + desiredStatusCode,
            expectedErrorCode,
            "An error occurred that resulted in response status code " + desiredStatusCode,
            desiredStatusCode
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_handles_MethodNotAllowedException_as_expected(
        boolean supportedMethodsIsEmpty
    ) {
        // given
        String actualMethod = UUID.randomUUID().toString();
        List<HttpMethod> supportedMethods =
            (supportedMethodsIsEmpty)
            ? Collections.emptyList()
            : Arrays.asList(
                HttpMethod.GET,
                HttpMethod.POST
            );

        MethodNotAllowedException ex = new MethodNotAllowedException(actualMethod, supportedMethods);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        // They throw the supported methods into a plain HashSet so we can't rely on the ordering.
        //      Verify it another way.
        Optional<String> supportedMethodsLoggingDetailsValue = result.extraDetailsForLogging
            .stream()
            .filter(p -> p.getKey().equals("supported_methods"))
            .map(Pair::getValue)
            .findAny();
        assertThat(supportedMethodsLoggingDetailsValue).isPresent();
        List<HttpMethod> actualLoggingDetailsMethods = supportedMethodsLoggingDetailsValue
            .map(s -> {
                if (s.isEmpty()) {
                    return Collections.<HttpMethod>emptyList();
                }
                return Arrays.stream(s.split(",")).map(HttpMethod::valueOf).collect(Collectors.toList());
            })
            .orElse(Collections.emptyList());

        assertThat(actualLoggingDetailsMethods).containsExactlyInAnyOrderElementsOf(supportedMethods);

        expectedExtraDetailsForLogging.add(Pair.of("supported_methods", supportedMethodsLoggingDetailsValue.get()));

        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getMethodNotAllowedApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_handles_NotAcceptableStatusException_as_expected(
        boolean includesSupportedMediaTypes
    ) {
        // given
        List<MediaType> supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.IMAGE_JPEG
        );
        NotAcceptableStatusException ex =
            (includesSupportedMediaTypes)
            ? new NotAcceptableStatusException(supportedMediaTypes)
            : new NotAcceptableStatusException("Some reason");

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedSupportedMediaTypesValueStr =
            (includesSupportedMediaTypes)
            ? supportedMediaTypes.stream().map(Object::toString).collect(Collectors.joining(","))
            : "";

        expectedExtraDetailsForLogging.add(Pair.of("supported_media_types", expectedSupportedMediaTypesValueStr));

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getNoAcceptableRepresentationApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_handles_ServerErrorException_as_expected(
        boolean nullDetails
    ) throws NoSuchMethodException {
        // given
        MethodParameter details = new MethodParameter(String.class.getDeclaredMethod("length"), -1);

        ServerErrorException ex =
            (nullDetails)
            ? new ServerErrorException("Some reason", null)
            : new ServerErrorException("Some reason", details, null);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.add(
            Pair.of("method_parameter", String.valueOf(ex.getMethodParameter()))
        );
        expectedExtraDetailsForLogging.add(
            Pair.of("handler_method", String.valueOf(ex.getHandlerMethod()))
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getGenericServiceError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_handles_ServerWebInputException_as_expected(
        boolean nullDetails
    ) throws NoSuchMethodException {
        // given
        MethodParameter details = new MethodParameter(String.class.getDeclaredMethod("length"), -1);

        ServerWebInputException ex =
            (nullDetails)
            ? new ServerWebInputException("Some reason")
            : new ServerWebInputException("Some reason", details);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.add(
            Pair.of("method_parameter", String.valueOf(ex.getMethodParameter()))
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getGenericBadRequestApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @SuppressWarnings("unused")
    public void methodWithAnnotatedParams(
        @RequestHeader int headerParam,
        @RequestParam int queryParam,
        @RequestHeader @RequestParam int bothParam,
        int unknownParam
    ) {
        // This method is used as part of some other tests, for generating the necessary MethodParameter objects.
    }

    @DataProvider(value = {
        "header                 |   0",
        "query_param            |   1",
        "header,query_param     |   2",
        "unknown                |   3"
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_handles_MissingRequestValueException_as_expected(
        String missingValueType, int paramIndex
    ) throws NoSuchMethodException {
        // given
        Method method = this.getClass()
                            .getDeclaredMethod("methodWithAnnotatedParams", int.class, int.class, int.class, int.class);
        MethodParameter details = new MethodParameter(
            method,
            paramIndex
        );

        String missingParamName = "some-param-" + UUID.randomUUID();
        MissingRequestValueException ex = new MissingRequestValueException(
            missingParamName,
            int.class,
            "blah not used",
            details
        );

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.add(
            Pair.of("method_parameter", String.valueOf(ex.getMethodParameter()))
        );

        Map<String, Object> expectedMetadata = new LinkedHashMap<>();
        expectedMetadata.put("missing_param_name", missingParamName);
        expectedMetadata.put("missing_param_type", "int");
        if (!"unknown".equals(missingValueType)) {
            expectedMetadata.put("required_location", missingValueType);
        }

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

    @DataProvider(value = {
        "header                 |   0",
        "query_param            |   1",
        "header,query_param     |   2",
        "unknown                |   3"
    }, splitBy = "\\|")
    @Test
    public void shouldHandleException_handles_MethodArgumentTypeMismatchException_as_expected_for_annotated_params(
        String badValueSource, int paramIndex
    ) throws NoSuchMethodException {
        // given
        Method method = this.getClass()
                            .getDeclaredMethod("methodWithAnnotatedParams", int.class, int.class, int.class, int.class);
        MethodParameter details = new MethodParameter(
            method,
            paramIndex
        );

        String badParamName = "some-param-" + UUID.randomUUID();
        String badParamValue = "some-bad-value-" + UUID.randomUUID();
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            badParamValue,
            int.class,
            badParamName,
            details,
            new RuntimeException("some cause")
        );

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.addAll(Arrays.asList(
            Pair.of("bad_property_name", badParamName),
            Pair.of("bad_property_value", badParamValue),
            Pair.of("required_type", "int"),
            Pair.of("method_arg_name", badParamName),
            Pair.of("method_arg_target_param", String.valueOf(ex.getParameter()))
        ));

        Map<String, Object> expectedMetadata = new LinkedHashMap<>();
        expectedMetadata.put("bad_property_name", badParamName);
        expectedMetadata.put("bad_property_value", badParamValue);
        expectedMetadata.put("required_type", "int");
        if (!"unknown".equals(badValueSource)) {
            expectedMetadata.put("required_location", badValueSource);
        }

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(new ApiErrorWithMetadata(
                testProjectApiErrors.getTypeConversionApiError(),
                expectedMetadata
            )),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_handles_UnsupportedMediaTypeStatusException_as_expected(
        boolean includeDetails
    ) {
        // given
        MediaType actualMediaType = MediaType.TEXT_PLAIN;
        List<MediaType> supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.IMAGE_JPEG
        );
        ResolvableType javaBodyType = ResolvableType.forClass(Integer.class);
        UnsupportedMediaTypeStatusException ex =
            (includeDetails)
            ? new UnsupportedMediaTypeStatusException(actualMediaType, supportedMediaTypes, javaBodyType)
            : new UnsupportedMediaTypeStatusException("Some reason");

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedSupportedMediaTypesValueStr =
            (includeDetails)
            ? supportedMediaTypes.stream().map(Object::toString).collect(Collectors.joining(","))
            : "";
        String expectedJavaBodyTypeValueStr =
            (includeDetails)
            ? javaBodyType.toString()
            : "null";

        expectedExtraDetailsForLogging.add(Pair.of("supported_media_types", expectedSupportedMediaTypesValueStr));
        expectedExtraDetailsForLogging.add(Pair.of("java_body_type", expectedJavaBodyTypeValueStr));

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getUnsupportedMediaTypeApiError()),
            expectedExtraDetailsForLogging
        );
    }

    private enum ConcatenateCollectionToStringScenario {
        NULL_COLLECTION(null, ""),
        EMPTY_COLLECTION(Collections.emptyList(), ""),
        SINGLE_ITEM(Collections.singleton("foo"), "foo"),
        MULTIPLE_ITEMS(Arrays.asList("foo", "bar"), "foo,bar");

        public final Collection<String> collection;
        public final String expectedResult;

        ConcatenateCollectionToStringScenario(Collection<String> collection, String expectedResult) {
            this.collection = collection;
            this.expectedResult = expectedResult;
        }
    }

    @DataProvider
    public static List<List<ConcatenateCollectionToStringScenario>> concatenateCollectionToStringScenarioDataProvider() {
        return Stream.of(ConcatenateCollectionToStringScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("concatenateCollectionToStringScenarioDataProvider")
    @Test
    public void concatenateCollectionToString_works_as_expected(ConcatenateCollectionToStringScenario scenario) {
        // when
        String result = listener.concatenateCollectionToString(scenario.collection);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
    }
}
