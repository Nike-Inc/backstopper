package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Runs the {@link ProjectApiErrorsTestBase} tests on a default {@link ProjectApiErrors}. Helps catch when the tests need to change
 * due to legitimate changes to the code.
 */
public class ProjectApiErrorsTestBaseTest extends ProjectApiErrorsTestBase {

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    }

    @Test
    public void shouldNotContainDuplicateNamedApiErrors_blows_up_if_it_finds_duplicate_ApiErrors() {
        // given
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return ProjectApiErrorsForTesting.withProjectSpecificData(Arrays.asList(
                    new ApiErrorBase("DUPNAME1", 42, "foo", 400),
                    new ApiErrorBase("DUPNAME1", 4242, "bar", 500),
                    new ApiErrorBase("DUPNAME2", 52, "foo2", 401),
                    new ApiErrorBase("DUPNAME2", 5252, "bar2", 501),
                    new ApiErrorBase("DUPNAME2", 525252, "baz", 900)
                ), ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES);
            }
        };

        // when
        Throwable ex = catchThrowable(base::shouldNotContainDuplicateNamedApiErrors);

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[DUPNAME1, 2], [DUPNAME2, 3]");
    }


    @Test
    public void findRandomApiErrorWithHttpStatusCode_throws_IllegalStateException_if_it_cannot_find_error_with_specified_status_code() {
        // given
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
            }
        };

        // when
        Throwable ex = catchThrowable(() -> base.findRandomApiErrorWithHttpStatusCode(42424242));

        // then
        assertThat(ex).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void verifyGetStatusCodePriorityOrderMethodContainsAllRelevantCodes_throws_AssertionError_if_it_finds_bad_state() {
        // given
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return ProjectApiErrorsForTesting.withProjectSpecificData(Collections.singletonList(
                    new ApiErrorBase("FOOBAR", 42, "foo", 123456)
                ), ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES);
            }
        };

        // when
        Throwable ex = catchThrowable(base::verifyGetStatusCodePriorityOrderMethodContainsAllRelevantCodes);

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("getStatusCodePriorityOrder() did not contain HTTP Status Code: 123456");
    }

    @Test
    public void allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange_works_for_valid_cases() {
        // given
        final ApiError coreError = BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR;
        final ApiError coreErrorWrapper = new ApiErrorBase(coreError, "FOOBAR");
        final String customErrorCode = UUID.randomUUID().toString();
        final ApiError validErrorInRange = new ApiErrorBase("WHEEE", customErrorCode, "whee message", 400);
        final ProjectSpecificErrorCodeRange restrictiveErrorCodeRange = new ProjectSpecificErrorCodeRange() {
            @Override
            public boolean isInRange(ApiError error) {
                return customErrorCode.equals(error.getErrorCode());
            }

            @Override
            public String getName() {
                return "RANGE_FOR_TESTING";
            }
        };
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return ProjectApiErrorsForTesting.withProjectSpecificData(Arrays.asList(coreErrorWrapper, validErrorInRange),
                                                                          restrictiveErrorCodeRange);
            }
        };

        // when
        Throwable ex = catchThrowable(
            base::allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange
        );

        // then
        assertThat(ex).isNull();
    }

    @Test
    public void allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange_should_explode_if_there_are_non_core_errors_and_range_is_null() {
        // given
        final ApiError nonCoreError = new ApiErrorBase("FOO", UUID.randomUUID().toString(), "foo message", 500);
        final List<ApiError> coreErrors = Arrays.asList(BarebonesCoreApiErrorForTesting.values());
        final List<ApiError> allProjectErrors = new ArrayList<>(coreErrors);
        allProjectErrors.add(nonCoreError);
        final ProjectApiErrors projectApiErrorsMock = mock(ProjectApiErrors.class);
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return projectApiErrorsMock;
            }
        };
        doReturn(null).when(projectApiErrorsMock).getProjectSpecificErrorCodeRange();
        doReturn(coreErrors).when(projectApiErrorsMock).getCoreApiErrors();
        doReturn(false).when(projectApiErrorsMock).isWrapperAroundCoreError(any(ApiError.class), anyList());
        doReturn(allProjectErrors).when(projectApiErrorsMock).getProjectApiErrors();

        // when
        Throwable ex = catchThrowable(
            base::allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange
        );

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(nonCoreError.getErrorCode());
    }

    @Test
    public void allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange_should_explode_if_there_are_non_core_errors_that_are_also_not_in_range() {
        // given
        final ApiError nonCoreError = new ApiErrorBase("FOO", UUID.randomUUID().toString(), "foo message", 500);
        final List<ApiError> coreErrors = Arrays.asList(BarebonesCoreApiErrorForTesting.values());
        final List<ApiError> allProjectErrors = new ArrayList<>(coreErrors);
        allProjectErrors.add(nonCoreError);
        final ProjectApiErrors projectApiErrorsMock = mock(ProjectApiErrors.class);
        final ProjectApiErrorsTestBase base = new ProjectApiErrorsTestBase() {
            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return projectApiErrorsMock;
            }
        };
        ProjectSpecificErrorCodeRange range = new ProjectSpecificErrorCodeRange() {
            @Override
            public boolean isInRange(ApiError error) {
                return false;
            }

            @Override
            public String getName() {
                return "RANGE_FOR_TESTING";
            }
        };
        doReturn(range).when(projectApiErrorsMock).getProjectSpecificErrorCodeRange();
        doReturn(coreErrors).when(projectApiErrorsMock).getCoreApiErrors();
        doReturn(false).when(projectApiErrorsMock).isWrapperAroundCoreError(any(ApiError.class), anyList());
        doReturn(allProjectErrors).when(projectApiErrorsMock).getProjectApiErrors();

        // when
        Throwable ex = catchThrowable(
            base::allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange
        );

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(nonCoreError.getErrorCode());
    }
}