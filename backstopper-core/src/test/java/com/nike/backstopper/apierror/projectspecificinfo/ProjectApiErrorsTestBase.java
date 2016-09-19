package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Verifies the functionality of the {@link ProjectApiErrors} helper methods and objects.
 * <p/>
 * <strong>NOTE: THIS IS A COPY-PASTE OF THE SAME-NAMED CLASS FROM THE backstopper-reusable-tests, SO IF YOU CHANGE THIS THEN YOU PROBABLY NEED TO CHANGE THAT CLASS ALSO!</strong>
 * Yes, this is crappy, but it's the least crappy of a series of crappy alternatives. backstopper-reusable-tests needs to expose the reusable tests (like this one)
 * as a non-test-jar so that transitive dependencies are resolved when the reusable tests are used by other projects, and so that gradle will actually pull the sources.
 * But at the same time this class also needs to be run here in this module because this is where {@link ProjectApiErrors} is defined.
 *
 * @author Nic Munroe
 */
public abstract class ProjectApiErrorsTestBase {

    protected ApiError findRandomApiErrorWithHttpStatusCode(int httpStatusCode) {
        for (ApiError error : getProjectApiErrors().getProjectApiErrors()) {
            if (error.getHttpStatusCode() == httpStatusCode)
                return error;
        }
        throw new IllegalStateException("Couldn't find ApiError with HTTP status code: " + httpStatusCode);
    }

    protected abstract ProjectApiErrors getProjectApiErrors();

    @Test
    public void verifyGetStatusCodePriorityOrderMethodContainsAllRelevantCodes() {
        for (ApiError error : getProjectApiErrors().getProjectApiErrors()) {
            int relevantCode = error.getHttpStatusCode();
            boolean containsRelevantCode = getProjectApiErrors().getStatusCodePriorityOrder().contains(relevantCode);
            if (!containsRelevantCode)
                throw new AssertionError("getStatusCodePriorityOrder() did not contain HTTP Status Code: " + relevantCode + " for " + getProjectApiErrors().getClass().getName() + "'s ApiError: " + error);
        }
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnNullForNullErrorCollection() {
        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(null), nullValue());
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnNullForEmptyErrorCollection() {
        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(Collections.<ApiError>emptyList()), nullValue());
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnTheSameValueRegardlessOfErrorOrder() {
        List<ApiError> list = Arrays.asList(
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(0)),
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(1)));

        int returnValNormalOrder = getProjectApiErrors().determineHighestPriorityHttpStatusCode(list);
        Collections.reverse(list);

        int returnValReverseOrder = getProjectApiErrors().determineHighestPriorityHttpStatusCode(list);
        assertThat(returnValNormalOrder, is(returnValReverseOrder));
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnTheCorrectValueWithAMixedList() {
        List<ApiError> list = new ArrayList<>(Arrays.asList(
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(2)),
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(3))));

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(list), is(getProjectApiErrors().getStatusCodePriorityOrder().get(2)));

        list.add(findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder().get(1)));

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(list), is(getProjectApiErrors().getStatusCodePriorityOrder().get(1)));
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnNullIfNoApiErrorsYouPassItHasHttpStatusCodeInPriorityOrderList() {
        ApiError mockApiError1 = mock(ApiError.class);
        ApiError mockApiError2 = mock(ApiError.class);
        doReturn(414141).when(mockApiError1).getHttpStatusCode();
        doReturn(424242).when(mockApiError2).getHttpStatusCode();
        List<ApiError> list = Arrays.asList(mockApiError1, mockApiError2);

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(list), nullValue());
    }

    @Test
    public void
    determineHighestPriorityHttpStatusCodeShouldReturnStatusCodeIfAtLeastOneApiErrorInListYouPassItHasHttpStatusCodeInPriorityOrderList() {
        ApiError mockApiError1 = mock(ApiError.class);
        ApiError mockApiError2 = mock(ApiError.class);
        doReturn(424242).when(mockApiError1).getHttpStatusCode();
        doReturn(400).when(mockApiError2).getHttpStatusCode();
        List<ApiError> list = Arrays.asList(mockApiError1, mockApiError2);

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(list), is(400));
    }

    @Test
    public void determineHighestPriorityHttpStatusCodeShouldReturnStatusCodeIfOnlyApiError() {
        ApiError mockApiError = mock(ApiError.class);
        doReturn(400).when(mockApiError).getHttpStatusCode();

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(Collections.singleton(mockApiError)),
                   is(400));
    }

    @Test
    public void
    determineHighestPriorityHttpStatusCodeShouldReturnStatusCodeIfOnlyApiErrorEvenIfNotInPriorityOrderList() {
        ApiError mockApiError = mock(ApiError.class);
        doReturn(424242).when(mockApiError).getHttpStatusCode();

        assertThat(getProjectApiErrors().determineHighestPriorityHttpStatusCode(Collections.singleton(mockApiError)),
                   is(424242));
    }

    @Test
    public void getSublistContainingOnlyHttpStatusCodeShouldReturnEmptyListForNullErrorCollection() {
        assertThat(getProjectApiErrors().getSublistContainingOnlyHttpStatusCode(null, getProjectApiErrors().getStatusCodePriorityOrder()
                                                                                                           .get(0)).size(), is(0));
    }

    @Test
    public void getSublistContainingOnlyHttpStatusCodeShouldReturnEmptyListForNullStatusCode() {
        ApiError randomError = getProjectApiErrors().getProjectApiErrors().get(0);
        assertThat(getProjectApiErrors().getSublistContainingOnlyHttpStatusCode(Collections.singletonList(randomError), null).size(), is(0));
    }

    @Test
    public void getSublistContainingOnlyHttpStatusCodeShouldFilterOutExpectedValues() {
        List<ApiError> mixedList = Arrays.asList(
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(0)),
            findRandomApiErrorWithHttpStatusCode(getProjectApiErrors().getStatusCodePriorityOrder()
                                                                      .get(1)));

        List<ApiError> filteredList = getProjectApiErrors().getSublistContainingOnlyHttpStatusCode(mixedList, getProjectApiErrors().getStatusCodePriorityOrder()
                                                                                                                                   .get(1));
        for(ApiError error : filteredList) {
            assertThat(error.getHttpStatusCode(), is(getProjectApiErrors().getStatusCodePriorityOrder().get(1)));
        }
    }

    @Test
    public void convertToApiErrorShouldReturnNullIfYouPassItNull() {
        assertThat(getProjectApiErrors().convertToApiError(null), nullValue());
    }

    @Test
    public void convertToApiErrorShouldReturnExpectedResultIfPassedValidNames() {
        for (ApiError apiError : getProjectApiErrors().getProjectApiErrors()) {
            assertThat("Did not get back the same instance for ApiError with name: " + apiError.getName() + ". This is usually because you have duplicate ApiError names - see the " +
                       "output of the shouldNotContainDuplicateNamedApiErrors() test to be sure. If that's not the case then you'll probably need to do some breakpoint debugging.",
                       getProjectApiErrors().convertToApiError(apiError.getName()), is(apiError));
        }
    }

    @Test
    public void convertToApiErrorShouldReturnNullIfYouPassItGarbage() {
        assertThat(getProjectApiErrors().convertToApiError(UUID.randomUUID().toString()), nullValue());
    }

    @Test
    public void convertToApiErrorShouldUseFallbackOnNullValue() {
        ApiError fallback = BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
        assertThat(getProjectApiErrors().convertToApiError(null, fallback), is(fallback));
    }

    @Test
    public void convertToApiErrorShouldUseFallbackOnInvalidValue() {
        ApiError fallback = BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
        assertThat(getProjectApiErrors().convertToApiError("notavaliderror", fallback), is(fallback));
    }

    @Test(expected = IllegalStateException.class)
    public void verifyErrorsAreInRangeShouldThrowExceptionIfListIncludesNonCoreApiErrorAndRangeIsNull() {
        ProjectApiErrorsForTesting.withProjectSpecificData(Collections.<ApiError>singletonList(new ApiErrorBase("blah", 99001, "stuff", 400)), null);
    }

    @Test
    public void verifyErrorsAreInRangeShouldNotThrowExceptionIfListIncludesCoreApiErrors() {
        ProjectApiErrors pae = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

        assertThat(pae, notNullValue());
        assertThat(pae.getProjectApiErrors().contains(BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR), is(true));
    }

    @Test
    public void verifyErrorsAreInRangeShouldNotThrowExceptionIfListIncludesCoreApiErrorWrapper() {
        ApiError coreApiError = BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
        final ApiError coreApiErrorWrapper = new ApiErrorBase("blah", coreApiError.getErrorCode(), coreApiError.getMessage(), coreApiError.getHttpStatusCode());
        ProjectApiErrors pae = ProjectApiErrorsForTesting.withProjectSpecificData(Collections.singletonList(coreApiErrorWrapper), null);

        assertThat(pae, notNullValue());
        assertThat(pae.getProjectApiErrors().contains(coreApiErrorWrapper), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void verifyErrorsAreInRangeShouldThrowExceptionIfListIncludesErrorOutOfRange() {
        ProjectApiErrorsForTesting.withProjectSpecificData(
            Collections.<ApiError>singletonList(new ApiErrorBase("blah", 1, "stuff", 400)),
            new ProjectSpecificErrorCodeRange() {
                @Override
                public boolean isInRange(ApiError error) {
                    return "42".equals(error.getErrorCode());
                }

                @Override
                public String getName() {
                    return "test error range";
                }
            }
        );
    }

    @Test
    public void shouldNotContainDuplicateNamedApiErrors() {
        Map<String, Integer> nameToCountMap = new HashMap<>();
        SortedSet<String> duplicateErrorNames = new TreeSet<>();
        for (ApiError apiError : getProjectApiErrors().getProjectApiErrors()) {
            Integer currentCount = nameToCountMap.get(apiError.getName());
            if (currentCount == null)
                currentCount = 0;

            Integer newCount = currentCount + 1;
            nameToCountMap.put(apiError.getName(), newCount);
            if (newCount > 1)
                duplicateErrorNames.add(apiError.getName());
        }

        if (!duplicateErrorNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("There are ApiError instances in the ProjectApiErrors that share duplicate names. [name, count]: ");
            boolean first = true;
            for (String dup : duplicateErrorNames) {
                if (!first)
                    sb.append(", ");

                sb.append("[").append(dup).append(", ").append(nameToCountMap.get(dup)).append("]");

                first = false;
            }

            throw new AssertionError(sb.toString());
        }
    }

    /**
     * Override this if the should_not_contain_same_error_codes_for_different_instances_that_are_not_wrappers test is
     * failing and you *really* want to allow one or more of your error codes to have duplicate ApiErrors that are
     * not wrappers. This should be used with care.
     */
    protected Set<String> allowedDuplicateErrorCodes() {
        return Collections.emptySet();
    }

    @Test
    public void should_not_contain_same_error_codes_for_different_instances_that_are_not_wrappers() {
        Set<String> allowedDuplicateErrorCodes = allowedDuplicateErrorCodes();
        Map<String, ApiError> codeToErrorMap = new HashMap<>();
        for (ApiError apiError : getProjectApiErrors().getProjectApiErrors()) {
            ApiError errorWithSameCode = codeToErrorMap.get(apiError.getErrorCode());

            if (errorWithSameCode != null && !areWrappersOfEachOther(apiError, errorWithSameCode)
                && !allowedDuplicateErrorCodes.contains(apiError.getErrorCode()))
            {
                throw new AssertionError(
                    "There are ApiError instances in the ProjectApiErrors that share duplicate error codes and are not "
                    + "wrappers of each other. error_code=" + apiError.getErrorCode() + ", conflicting_api_errors=["
                    + apiError.getName() + ", " + errorWithSameCode.getName() + "]"
                );
            }

            codeToErrorMap.put(apiError.getErrorCode(), apiError);
        }
    }

    private boolean areWrappersOfEachOther(ApiError error1, ApiError error2) {
        boolean errorCodeMatches = Objects.equals(error1.getErrorCode(), error2.getErrorCode());
        boolean messageMatches = Objects.equals(error1.getMessage(), error2.getMessage());
        boolean httpStatusCodeMatches = error1.getHttpStatusCode() == error2.getHttpStatusCode();
        if (errorCodeMatches && messageMatches && httpStatusCodeMatches) {
            return true;
        }

        return false;
    }

    @Test
    public void allErrorsShouldBeCoreApiErrorsOrCoreApiErrorWrappersOrFallInProjectSpecificErrorRange() {
        ProjectSpecificErrorCodeRange projectSpecificErrorCodeRange = getProjectApiErrors().getProjectSpecificErrorCodeRange();

        for (ApiError error : getProjectApiErrors().getProjectApiErrors()) {
            boolean valid = false;
            if (getProjectApiErrors().getCoreApiErrors().contains(error) || getProjectApiErrors().isWrapperAroundCoreError(error, getProjectApiErrors().getCoreApiErrors()))
                valid = true;
            else if (projectSpecificErrorCodeRange != null && projectSpecificErrorCodeRange.isInRange(error))
                valid = true;

            if (!valid) {
                throw new AssertionError("Found an ApiError in the ProjectApiErrors that is not a core error or wrapper around a core error, and its error code does not fall in the  " +
                                         "range of getProjectApiErrors().getProjectSpecificErrorCodeRange(). getProjectApiErrors().getProjectSpecificErrorCodeRange(): " + projectSpecificErrorCodeRange +
                                         ". Offending error info: name=" + error.getName() + ", errorCode=" + error.getErrorCode() + ", message=\"" + error.getMessage() + "\", httpStatusCode=" +
                                         error.getHttpStatusCode() + ", class=" + error.getClass().getName());
            }
        }
    }

}
