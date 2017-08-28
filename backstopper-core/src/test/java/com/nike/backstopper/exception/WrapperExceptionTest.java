package com.nike.backstopper.exception;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link WrapperException}
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class WrapperExceptionTest {

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void constructor_with_cause_works_as_expected(boolean useNullCause) {
        // given
        Throwable cause = (useNullCause) ? null : new Exception("boom");
        WrapperException ex = new WrapperException(cause);

        // expect
        if (useNullCause) {
            assertThat(ex.getCause()).isNull();
            assertThat(ex.getMessage()).isNull();
            assertThat(ex.toString()).isEqualTo(ex.getClass().getName());
        }
        else {
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).isEqualTo(cause.toString());
            assertThat(ex.toString()).endsWith(cause.toString());
        }
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void constructor_with_cause_and_message_works_as_expected(boolean useNullCause) {
        // given
        Throwable cause = (useNullCause) ? null : new Exception("boom");
        String message = UUID.randomUUID().toString();
        WrapperException ex = new WrapperException(message, cause);

        // expect
        if (useNullCause) {
            assertThat(ex.getCause()).isNull();
            assertThat(ex.getMessage()).isEqualTo(message);
            assertThat(ex.toString()).isEqualTo(ex.getClass().getName() + ": " + message);
        }
        else {
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).isEqualTo(message);
            assertThat(ex.toString()).endsWith(cause.toString());
        }
    }

    @DataProvider(value = {
        "true   |   true",
        "false  |   true",
        "true   |   false",
        "false  |   false"
    }, splitBy = "\\|")
    @Test
    public void toString_sets_and_uses_cache_value(
        boolean useNullMessage, boolean useNullCause
    ) {
        // given
        Throwable cause = (useNullCause) ? null : new Exception("boom");
        String message = (useNullMessage) ? null : UUID.randomUUID().toString();
        WrapperException ex = new WrapperException(message, cause);
        assertThat(ex.toStringCache).isNull();

        // when
        String origResult = ex.toString();

        // then
        assertThat(ex.toStringCache)
            .isNotNull()
            .isEqualTo(origResult);

        // and when
        ex.toStringCache = UUID.randomUUID().toString();
        String fromModifiedCacheResult = ex.toString();

        // then
        assertThat(fromModifiedCacheResult)
            .isEqualTo(ex.toStringCache)
            .isNotEqualTo(origResult);
    }
}