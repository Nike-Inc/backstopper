package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.nike.internal.util.Pair;
import com.nike.internal.util.testing.Glassbox;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
        Glassbox.setInternalState(listener, "projectApiErrors", testProjectApiErrors);
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
    public void shouldDefaultToGENERIC_SERVICE_ERRORIfMessageIsNotValidApiErrorEnumName() {
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = Collections.singletonList(new FieldError("someObj", "someField", UUID.randomUUID().toString()));
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        BindException ex = new BindException(bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getGenericServiceError()));
    }

}
