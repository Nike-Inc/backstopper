package com.nike.backstopper.handler.listener;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.internal.util.Pair;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * Tests the functionality of {@link ApiExceptionHandlerListenerResult}.
 *
 * @author Nic Munroe
 */
public class ApiExceptionHandlerListenerResultTest {

    @Test
    public void ignoreResponseShouldReturnInstanceWithCorrectValues() {
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.ignoreResponse();
        assertThat(val.shouldHandleResponse, is(false));
        assertThat(val.errors, notNullValue());
        assertThat(val.errors.isEmpty(), is(true));
        assertThat(val.extraDetailsForLogging, notNullValue());
        assertThat(val.extraDetailsForLogging.isEmpty(), is(true));
    }

    @Test
    public void handleResponseShouldReturnInstanceWithEmptyErrorsIfPassedNullErrorSet() {
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(null);
        assertThat(val.errors, notNullValue());
        assertThat(val.errors.isEmpty(), is(true));
    }

    @Test
    public void handleResponseShouldReturnInstanceWithEmptyLoggingDetailsIfPassedNullLoggingDetails() {
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(null, null);
        assertThat(val.extraDetailsForLogging, notNullValue());
        assertThat(val.extraDetailsForLogging.isEmpty(), is(true));
    }

    @Test
    public void handleResponseShouldReturnInstanceWithCorrectValues() {
        SortedApiErrorSet errorsSet = new SortedApiErrorSet(Arrays.<ApiError>asList(BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST,
                                                                                    BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT));
        List<Pair<String, String>> extraDetailsForLogging = Arrays.asList(Pair.of("logkey1", "logval1"), Pair.of("logkey2", "logval2"));
        ApiExceptionHandlerListenerResult val = ApiExceptionHandlerListenerResult.handleResponse(errorsSet, extraDetailsForLogging);

        assertThat(val.errors, containsInAnyOrder(errorsSet.toArray()));
        assertThat(val.extraDetailsForLogging, contains(extraDetailsForLogging.toArray()));
    }

}
