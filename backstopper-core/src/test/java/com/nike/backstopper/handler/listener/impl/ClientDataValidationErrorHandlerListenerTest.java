package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.ClientDataValidationError;
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
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.ConstraintDescriptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener}.
 *
 * @author Nic Munroe
 */
public class ClientDataValidationErrorHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

    private final ClientDataValidationErrorHandlerListener listener = new ClientDataValidationErrorHandlerListener(testProjectApiErrors,
                                                                                                                   ApiExceptionHandlerUtils.DEFAULT_IMPL);

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        ClientDataValidationErrorHandlerListener impl = new ClientDataValidationErrorHandlerListener(projectErrorsMock, utilsMock);

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
                new ClientDataValidationErrorHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL);
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
                new ClientDataValidationErrorHandlerListener(mock(ProjectApiErrors.class), null);
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
    public void shouldReturnGENERIC_SERVICE_ERRORForClientDataValidationErrorThatHasNullViolations() {
        ClientDataValidationError ex = new ClientDataValidationError(null, null, null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getGenericServiceError()));
    }

    @Test
    public void shouldReturnGENERIC_SERVICE_ERRORForClientDataValidationErrorThatHasEmptyViolations() {
        ClientDataValidationError ex = new ClientDataValidationError(null, Collections.emptyList(), null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getGenericServiceError()));
    }

    @Test
    public void shouldNotAddExtraLoggingDetailsForObjectWhenObjectListIsNull() {
        ClientDataValidationError ex = new ClientDataValidationError(null, null, null);

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processClientDataValidationError(ex, extraLoggingDetails);
        assertThat(extraLoggingDetails.isEmpty(), is(true));
    }

    @Test
    public void shouldNotAddExtraLoggingDetailsForObjectWhenObjectListIsEmpty() {
        ClientDataValidationError ex = new ClientDataValidationError(Collections.emptyList(), null, null);

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processClientDataValidationError(ex, extraLoggingDetails);
        assertThat(extraLoggingDetails.isEmpty(), is(true));
    }

    @Test
    public void shouldNotAddExtraLoggingDetailsForValidationGroupsWhenGroupsArrayIsNull() {
        ClientDataValidationError ex = new ClientDataValidationError(null, null, null);

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processClientDataValidationError(ex, extraLoggingDetails);
        assertThat(extraLoggingDetails.isEmpty(), is(true));
    }

    @Test
    public void shouldNotAddExtraLoggingDetailsForValidationGroupsWhenGroupsArrayIsEmpty() {
        ClientDataValidationError ex = new ClientDataValidationError(null, null, new Class<?>[0]);

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processClientDataValidationError(ex, extraLoggingDetails);
        assertThat(extraLoggingDetails.isEmpty(), is(true));
    }

    private ConstraintViolation<Object> setupConstraintViolation(Class offendingObjectClass, String path, Class<? extends Annotation> annotationClass, String message) {
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

        doReturn(offendingObjectClass).when(mockConstraintViolation).getRootBeanClass();

        return mockConstraintViolation;
    }

    @Test
    public void shouldReturnGENERIC_SERVICE_ERRORForViolationThatDoesNotMapToApiError() {
        ConstraintViolation<Object> violation = setupConstraintViolation(SomeValidatableObject.class, "path.to.violation", NotNull.class, "I_Am_Invalid");
        ClientDataValidationError ex = new ClientDataValidationError(
            List.of(new SomeValidatableObject("someArg1", "someArg2")), Collections.singletonList(violation), null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.<ApiError>singletonList(
            // We expect it to be the generic error, with some metadata about the field that had an issue
            new ApiErrorWithMetadata(
                testProjectApiErrors.getGenericServiceError(),
                Pair.of("field", "path.to.violation")
            )
        ));
    }

    @Test
    public void shouldReturnExpectedErrorsForViolationsThatMapToApiErrors() {
        ConstraintViolation<Object> violation1 = setupConstraintViolation(SomeValidatableObject.class, "path.to.violation1", NotNull.class, "MISSING_EXPECTED_CONTENT");
        ConstraintViolation<Object> violation2 = setupConstraintViolation(SomeValidatableObject.class, "path.to.violation2", NotEmpty.class, "TYPE_CONVERSION_ERROR");
        ClientDataValidationError ex = new ClientDataValidationError(
            Collections.singletonList(new SomeValidatableObject("someArg1", "someArg2")), Arrays.asList(violation1, violation2), null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Arrays.<ApiError>asList(
            // We expect them to be the properly associated errors, with some metadata about the field that had an issue
            new ApiErrorWithMetadata(BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT, Pair.of("field",
                                                                                                       "path.to.violation1"
            )),
            new ApiErrorWithMetadata(BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR, Pair.of("field",
                                                                                                    "path.to.violation2"
            ))
        ));
    }

    @Test
    public void shouldAddExtraLoggingDetailsForClientDataValidationError() {
        ConstraintViolation<Object> violation1 = setupConstraintViolation(SomeValidatableObject.class, "path.to.violation1", NotNull.class, "MISSING_EXPECTED_CONTENT");
        ConstraintViolation<Object> violation2 = setupConstraintViolation(Object.class, "path.to.violation2", NotEmpty.class, "TYPE_CONVERSION_ERROR");
        ClientDataValidationError ex = new ClientDataValidationError(
                Arrays.asList(new SomeValidatableObject("someArg1", "someArg2"), new Object()),
                Arrays.asList(violation1, violation2),
                new Class<?>[] {Default.class, SomeValidationGroup.class}
        );

        List<Pair<String, String>> extraLoggingDetails = new ArrayList<>();
        listener.processClientDataValidationError(ex, extraLoggingDetails);
        assertThat(extraLoggingDetails, containsInAnyOrder(
                Pair.of("client_data_validation_failed_objects", SomeValidatableObject.class.getName() + "," + Object.class.getName()),
                Pair.of("validation_groups_considered", Default.class.getName() + "," + SomeValidationGroup.class.getName()),
                Pair.of("constraint_violation_details",
                        "SomeValidatableObject.path.to.violation1|jakarta.validation.constraints.NotNull|MISSING_EXPECTED_CONTENT,Object.path.to.violation2|org.hibernate.validator.constraints" +
                                ".NotEmpty|TYPE_CONVERSION_ERROR"))
        );
    }

    private interface SomeValidationGroup {}

    private static class SomeValidatableObject {

        @NotEmpty(message = "MISSING_EXPECTED_CONTENT")
        private final String arg1;
        @NotEmpty(message = "MISSING_EXPECTED_CONTENT")
        private final String arg2;

        public SomeValidatableObject(String arg1, String arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }
    }

}