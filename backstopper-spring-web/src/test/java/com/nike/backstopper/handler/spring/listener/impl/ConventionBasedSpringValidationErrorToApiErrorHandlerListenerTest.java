package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.nike.internal.util.Pair;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ConventionBasedSpringValidationErrorToApiErrorHandlerListener}.
 *
 * @author Nic Munroe
 */
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
        Assertions.assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new ConventionBasedSpringValidationErrorToApiErrorHandlerListener(null);
            }
        });

        // then
        Assertions.assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new ApiException(testProjectApiErrors.getGenericServiceError())), false, null);
    }

    @Test
    public void shouldCreateValidationErrorsForMethodArgumentNotValidException() {
        MethodParameter methodParam = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = Collections.<ObjectError>singletonList(
            new FieldError("someObj", "someField", testProjectApiErrors.getMissingExpectedContentApiError().getName())
        );
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParam, bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Collections.singletonList(
            new ApiErrorWithMetadata(testProjectApiErrors.getMissingExpectedContentApiError(),
                                     Pair.of("field", (Object)"someField"))
        ));
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void shouldCreateValidationErrorsForBindException() {
        BindingResult bindingResult = mock(BindingResult.class);

        ApiError someFieldError = testProjectApiErrors.getMissingExpectedContentApiError();
        ApiError otherFieldError = testProjectApiErrors.getTypeConversionApiError();
        List<ObjectError> errorsList = Arrays.<ObjectError>asList(
                new FieldError("someObj", "someField", someFieldError.getName()),
                new FieldError("otherObj", "otherField", otherFieldError.getName()));
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        BindException ex = new BindException(bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Arrays.asList(
            new ApiErrorWithMetadata(someFieldError, Pair.of("field", (Object)"someField")),
            new ApiErrorWithMetadata(otherFieldError, Pair.of("field", (Object)"otherField"))
        ));
        verify(bindingResult).getAllErrors();
    }

    @Test
    public void shouldDefaultToGENERIC_SERVICE_ERRORIfMessageIsNotValidApiErrorEnumName() {
        BindingResult bindingResult = mock(BindingResult.class);

        List<ObjectError> errorsList = Collections.<ObjectError>singletonList(new FieldError("someObj", "someField", UUID.randomUUID().toString()));
        when(bindingResult.getAllErrors()).thenReturn(errorsList);

        BindException ex = new BindException(bindingResult);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getGenericServiceError()));
    }

}
