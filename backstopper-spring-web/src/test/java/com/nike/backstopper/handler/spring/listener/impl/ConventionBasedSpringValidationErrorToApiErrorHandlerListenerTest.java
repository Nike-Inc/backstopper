package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.nike.backstopper.handler.spring.listener.impl.ConventionBasedSpringValidationErrorToApiErrorHandlerListener.WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME;
import static com.nike.backstopper.handler.spring.listener.impl.ConventionBasedSpringValidationErrorToApiErrorHandlerListener.extractGetAllErrorsMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ConventionBasedSpringValidationErrorToApiErrorHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ConventionBasedSpringValidationErrorToApiErrorHandlerListenerTest extends ListenerTestBase {

    private static ConventionBasedSpringValidationErrorToApiErrorHandlerListener listener;
    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

    @BeforeClass
    public static void setupClass() {
        listener = new ConventionBasedSpringValidationErrorToApiErrorHandlerListener(testProjectApiErrors);
        Whitebox.setInternalState(listener, "projectApiErrors", testProjectApiErrors);
    }

    @Test
    public void constructor_sets_projectApiErrors_to_passed_in_arg() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);

        // when
        ConventionBasedSpringValidationErrorToApiErrorHandlerListener impl = new ConventionBasedSpringValidationErrorToApiErrorHandlerListener(projectErrorsMock);

        // then
        assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new ConventionBasedSpringValidationErrorToApiErrorHandlerListener(null)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void const_WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME_equals_WebExchangeBindException() {
        // expect
        assertThat(WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME).isEqualTo(WebExchangeBindException.class.getName());
    }

    @Test
    public void extractGetAllErrorsMethod_works_as_expected_for_WebExchangeBindException()
        throws NoSuchMethodException {
        
        // when
        Method result = extractGetAllErrorsMethod(WEB_EXCHANGE_BIND_EXCEPTION_CLASSNAME);

        // then
        assertThat(result).isEqualTo(WebExchangeBindException.class.getDeclaredMethod("getAllErrors"));
    }

    @Test
    public void extractGetAllErrorsMethod_returns_null_if_class_not_found() {
        // expect
        assertThat(extractGetAllErrorsMethod("does.not.exist.Foo")).isNull();
    }

    @Test
    public void extractGetAllErrorsMethod_returns_null_for_unexpected_exception() {
        // expect
        assertThat(extractGetAllErrorsMethod(this.getClass().getName())).isNull();
    }

    @Test
    public void shouldHandleException_gracefully_ignores_when_the_exception_is_null() {
        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(null);

        // then
        validateResponse(result, false, null);
    }

    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new ApiException(testProjectApiErrors.getGenericServiceError())), false, null);
    }

    @Test
    public void shouldCreateValidationErrorsForMethodArgumentNotValidException() {
        MethodParameter methodParam = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = Collections.singletonList(
            new FieldError("someObj", "someField", testProjectApiErrors.getMissingExpectedContentApiError().getName())
        );
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Collections.singletonList(
            new ApiErrorWithMetadata(testProjectApiErrors.getMissingExpectedContentApiError(),
                                     Pair.of("field", "someField"))
        ));
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void shouldCreateValidationErrorsForBindException() {
        BindingResult bindingResult = mock(BindingResult.class);

        ApiError someFieldError = testProjectApiErrors.getMissingExpectedContentApiError();
        ApiError otherFieldError = testProjectApiErrors.getTypeConversionApiError();
        ApiError notAFieldError = testProjectApiErrors.getGenericBadRequestApiError();
        List<ObjectError> errorsList = Arrays.asList(
                new FieldError("someObj", "someField", someFieldError.getName()),
                new FieldError("otherObj", "otherField", otherFieldError.getName()),
                new ObjectError("notAFieldObject", notAFieldError.getName())
        );
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        BindException ex = new BindException(bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Arrays.asList(
            new ApiErrorWithMetadata(someFieldError, Pair.of("field", "someField")),
            new ApiErrorWithMetadata(otherFieldError, Pair.of("field", "otherField")),
            notAFieldError
        ));
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void shouldHandleException_handles_WebExchangeBindException_as_expected() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);

        ApiError someFieldError = testProjectApiErrors.getMissingExpectedContentApiError();
        ApiError otherFieldError = testProjectApiErrors.getTypeConversionApiError();
        ApiError notAFieldError = testProjectApiErrors.getGenericBadRequestApiError();
        List<ObjectError> errorsList = Arrays.asList(
            new FieldError("someObj", "someField", someFieldError.getName()),
            new FieldError("otherObj", "otherField", otherFieldError.getName()),
            new ObjectError("notAFieldObject", notAFieldError.getName())
        );
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        WebExchangeBindException ex = new WebExchangeBindException(null, bindingResult);

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, Arrays.asList(
            new ApiErrorWithMetadata(someFieldError, Pair.of("field", "someField")),
            new ApiErrorWithMetadata(otherFieldError, Pair.of("field", "otherField")),
            notAFieldError
        ));
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void shouldHandleException_delegates_to_extractAllErrorsFromWebExchangeBindException_for_WebExchangeBindException_error_retrieval() {
        // given
        ConventionBasedSpringValidationErrorToApiErrorHandlerListener listenerSpy = spy(listener);

        WebExchangeBindException ex = new WebExchangeBindException(null, mock(BindingResult.class));

        ApiError someFieldError = testProjectApiErrors.getMissingExpectedContentApiError();
        ApiError otherFieldError = testProjectApiErrors.getTypeConversionApiError();
        ApiError notAFieldError = testProjectApiErrors.getGenericBadRequestApiError();
        List<ObjectError> errorsList = Arrays.asList(
            new FieldError("someObj", "someField", someFieldError.getName()),
            new FieldError("otherObj", "otherField", otherFieldError.getName()),
            new ObjectError("notAFieldObject", notAFieldError.getName())
        );

        doReturn(errorsList).when(listenerSpy).extractAllErrorsFromWebExchangeBindException(ex);

        // when
        ApiExceptionHandlerListenerResult result = listenerSpy.shouldHandleException(ex);

        // then
        validateResponse(result, true, Arrays.asList(
            new ApiErrorWithMetadata(someFieldError, Pair.of("field", "someField")),
            new ApiErrorWithMetadata(otherFieldError, Pair.of("field", "otherField")),
            notAFieldError
        ));
        verify(listenerSpy).extractAllErrorsFromWebExchangeBindException(ex);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void shouldHandleException_ignores_WebExchangeBindException_that_has_null_or_empty_ObjectError_list(
        boolean objectErrorsListIsNull
    ) {
        // given
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = (objectErrorsListIsNull) ? null : Collections.emptyList();
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        WebExchangeBindException ex = new WebExchangeBindException(null, bindingResult);

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, false, null);
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void extractAllErrorsFromWebExchangeBindException_works_as_expected_for_WebExchangeBindException() {
        // given
        WebExchangeBindException exMock = mock(WebExchangeBindException.class);
        List<ObjectError> expectedErrorsList = mock(List.class);

        doReturn(expectedErrorsList).when(exMock).getAllErrors();

        // when
        List<ObjectError> result = listener.extractAllErrorsFromWebExchangeBindException(exMock);

        // then
        assertThat(result).isSameAs(expectedErrorsList);
    }

    @Test
    public void extractAllErrorsFromWebExchangeBindException_returns_null_if_extraction_method_is_null() {
        // given
        ConventionBasedSpringValidationErrorToApiErrorHandlerListener listenerSpy = spy(listener);
        doReturn(null).when(listenerSpy).getWebExchangeBindExGetAllErrorsMethod();

        // when
        List<ObjectError> result = listenerSpy.extractAllErrorsFromWebExchangeBindException(
            mock(WebExchangeBindException.class)
        );

        // then
        assertThat(result).isNull();
        verify(listenerSpy).getWebExchangeBindExGetAllErrorsMethod();
    }
    @Test
    public void extractAllErrorsFromWebExchangeBindException_returns_null_if_unexpected_exception_occurs() {
        // when
        List<ObjectError> result = listener.extractAllErrorsFromWebExchangeBindException(new RuntimeException());

        // then
        assertThat(result).isNull();
    }

    @Test
    public void shouldDefaultToGENERIC_SERVICE_ERRORIfMessageIsNotValidApiErrorEnumName() {
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = Collections.singletonList(new FieldError("someObj", "someField", UUID.randomUUID().toString()));
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        BindException ex = new BindException(bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getGenericServiceError()));
    }

}
