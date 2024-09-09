package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.ServersideValidationError;
import com.nike.backstopper.exception.network.DownstreamRequestOrResponseBodyFailedValidationException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.metadata.ConstraintDescriptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ServersideValidationErrorHandlerListener}.
 *
 * @author Nic Munroe
 */
public class ServersideValidationErrorHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private ServersideValidationErrorHandlerListener listener = new ServersideValidationErrorHandlerListener(testProjectApiErrors,
                                                                                                             ApiExceptionHandlerUtils.DEFAULT_IMPL);

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        ServersideValidationErrorHandlerListener impl = new ServersideValidationErrorHandlerListener(projectErrorsMock, utilsMock);

        // then
        Assertions.assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        Assertions.assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new ServersideValidationErrorHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL);
            }
        });

        // then
        Assertions.assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new ServersideValidationErrorHandlerListener(mock(ProjectApiErrors.class), null);
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
    public void shouldReturnSERVERSIDE_VALIDATION_ERRORForServersideValidationError() {
        ServersideValidationError ex = new ServersideValidationError(null, null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getServersideValidationApiError()));
    }

    @Test
    public void shouldTreatDownstreamRequestOrResponseBodyFailedValidationExceptionExactlyLikeServersideValidationError() {
        ServersideValidationError sve = new ServersideValidationError(null, null);
        DownstreamRequestOrResponseBodyFailedValidationException ex = new DownstreamRequestOrResponseBodyFailedValidationException(sve, null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getServersideValidationApiError()));
    }

    @Test
    public void shouldIgnoreDownstreamRequestOrResponseBodyFailedValidationExceptionIfCauseIsNotAServersideValidationError() {
        DownstreamRequestOrResponseBodyFailedValidationException ex = new DownstreamRequestOrResponseBodyFailedValidationException(new Exception("intentional test exception"), null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, false, null);
    }

    private ConstraintViolation<Object> setupConstraintViolation(String path, Class<? extends Annotation> annotationClass, String message) {
        ConstraintViolation<Object> mockConstraintViolation = mock(ConstraintViolation.class);

        Path mockPath = mock(Path.class);
        doReturn(path).when(mockPath).toString();

        Annotation mockAnnotation = mock(Annotation.class);
        doReturn(annotationClass).when(mockAnnotation).annotationType();

        ConstraintDescriptor<?> mockConstraintDescriptor = mock(ConstraintDescriptor.class);
        doReturn(mockAnnotation).when(mockConstraintDescriptor).getAnnotation();

        when(mockConstraintViolation.getPropertyPath()).thenReturn(mockPath);
        doReturn(mockConstraintDescriptor).when(mockConstraintViolation).getConstraintDescriptor();
        when(mockConstraintViolation.getMessage()).thenReturn(message);

        return mockConstraintViolation;
    }

    @Test
    public void shouldAddExtraLoggingDetailsForServersideValidationError() {
        ConstraintViolation<Object> violation1 = setupConstraintViolation("path.to.violation1", NotNull.class, "Violation_1_Message");
        ConstraintViolation<Object> violation2 = setupConstraintViolation("path.to.violation2", NotEmpty.class, "Violation_2_Message");
        ServersideValidationError ex = new ServersideValidationError(new SomeValidatableObject("someArg1", "someArg2"), new LinkedHashSet<>(Arrays.asList(violation1, violation2)));

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processServersideValidationError(ex, extraLoggingDetails);
        extraLoggingDetails.toString();
        assertThat(extraLoggingDetails, containsInAnyOrder(Pair.of("serverside_validation_object", SomeValidatableObject.class.getName()),
                Pair.of("serverside_validation_errors",
                        "path.to.violation1|jakarta.validation.constraints.NotNull|Violation_1_Message, path.to.violation2|org.hibernate.validator.constraints" +
                                ".NotEmpty|Violation_2_Message")));
    }

    private static class SomeValidatableObject {

        @NotEmpty(message = "INVALID_TRUSTED_HEADERS_ERROR")
        private String arg1;
        @NotEmpty(message = "INVALID_TRUSTED_HEADERS_ERROR")
        private String arg2;

        public SomeValidatableObject(String arg1, String arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }
    }

}
