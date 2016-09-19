package com.nike.backstopper.exception;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests the functionality of {@link WrapperException}
 *
 * @author Nic Munroe
 */
public class WrapperExceptionTest {

    @Test
    public void constructor_with_cause_works_as_expected() {
        // given
        Throwable cause = new Exception("boom");
        WrapperException ex = new WrapperException(cause);

        // expect
        assertThat(ex.getCause(), sameInstance(cause));
    }

    @Test
    public void constructor_with_cause_and_message_works_as_expected() {
        // given
        Throwable cause = new Exception("boom");
        String message = UUID.randomUUID().toString();
        WrapperException ex = new WrapperException(message, cause);

        // expect
        assertThat(ex.getCause(), sameInstance(cause));
        assertThat(ex.getMessage(), is(message));
    }
}