package com.nike.backstopper.handler.spring.webflux.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;
import com.nike.internal.util.testing.Glassbox;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link OneOffSpringWebFluxFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringWebFluxFrameworkExceptionHandlerListenerTest {
    
    private static final ProjectApiErrors testProjectApiErrors =
        ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

    private OneOffSpringWebFluxFrameworkExceptionHandlerListener listener =
        new OneOffSpringWebFluxFrameworkExceptionHandlerListener(
            testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL
        );

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringWebFluxFrameworkExceptionHandlerListener
            impl = new OneOffSpringWebFluxFrameworkExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        assertThat(Glassbox.getInternalState(impl, "projectApiErrors")).isSameAs(projectErrorsMock);
        assertThat(Glassbox.getInternalState(impl, "utils")).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebFluxFrameworkExceptionHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebFluxFrameworkExceptionHandlerListener(mock(ProjectApiErrors.class), null)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle,
        Collection<? extends ApiError> expectedErrors,
        Pair<String, String> ... expectedExtraDetailsForLogging
    ) {
        List<Pair<String, String>> loggingDetailsList = (expectedExtraDetailsForLogging == null)
                                                        ? Collections.emptyList()
                                                        : Arrays.asList(expectedExtraDetailsForLogging);
        validateResponse(
            result, expectedShouldHandle, expectedErrors, loggingDetailsList
        );
    }

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle,
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

    @Test
    public void handleFluxExceptions_returns_ignoreResponse_if_passed_exception_it_does_not_want_to_handle() {
        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
            new ApiException(testProjectApiErrors.getGenericServiceError())
        );

        // then
        validateResponse(result, false, null);
    }

}