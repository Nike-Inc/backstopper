package com.nike.backstopper.handler.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Base class for the test classes testing the various {@link com.nike.backstopper.handler.listener.ApiExceptionHandlerListener} implementations.
 * provides common helper methods.
 *
 * @author Nic Munroe
 */
public abstract class ListenerTestBase {

    protected void validateResponse(ApiExceptionHandlerListenerResult result, boolean expectedShouldHandle, Collection<? extends ApiError> expectedErrors) {
        if (!expectedShouldHandle) {
            assertThat(result.shouldHandleResponse, is(false));
            return;
        }

        assertThat(result.errors.size(), is(expectedErrors.size()));
        for(ApiError error : expectedErrors) {
            assertTrue(result.errors.contains(error));
        }
    }

}
