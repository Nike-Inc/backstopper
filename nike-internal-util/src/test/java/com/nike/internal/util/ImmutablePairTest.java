package com.nike.internal.util;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link ImmutablePair}
 */
public class ImmutablePairTest {

    @Test
    public void constructor_works_as_expected() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();
        ImmutablePair<String, String> pair = new ImmutablePair<>(left, right);

        // expect
        assertThat(pair.getLeft(), is(left));
        assertThat(pair.getKey(), is(left));
        assertThat(pair.getRight(), is(right));
        assertThat(pair.getValue(), is(right));
    }

    @Test
    public void static_constructor_works_as_expected() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();
        ImmutablePair<String, String> pair = ImmutablePair.of(left, right);

        // expect
        assertThat(pair.getLeft(), is(left));
        assertThat(pair.getKey(), is(left));
        assertThat(pair.getRight(), is(right));
        assertThat(pair.getValue(), is(right));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setValue_throws_UnsupportedOperationException() {
        // given
        ImmutablePair<String, String> pair = ImmutablePair.of("foo", "bar");

        // expect
        pair.setValue("blowup");
    }
}