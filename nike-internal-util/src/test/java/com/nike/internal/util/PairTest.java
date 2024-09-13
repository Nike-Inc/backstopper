package com.nike.internal.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link Pair}
 */
@RunWith(DataProviderRunner.class)
public class PairTest {

    public static class PairForTests<L, R> extends Pair<L, R> {

        private final L left;
        private R right;

        public PairForTests(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public static <L, R> PairForTests<L, R> of(L left, R right) {
            return new PairForTests<>(left, right);
        }

        @Override
        public L getLeft() {
            return left;
        }

        @Override
        public R getRight() {
            return right;
        }

        @Override
        public R setValue(R value) {
            this.right = value;
            return right;
        }
    }

    @Test
    public void getters_work() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();
        Pair<String, String> pair = PairForTests.of(left, right);

        // expect
        assertThat(pair.getLeft(), is(left));
        assertThat(pair.getKey(), is(left));
        assertThat(pair.getRight(), is(right));
        assertThat(pair.getValue(), is(right));
    }

    @Test
    public void compareTo_returns_0_for_same_instance_comparison() {
        // given
        Pair<String, String> pair = PairForTests.of("foo", "bar");

        // expect
        //noinspection EqualsWithItself
        assertThat(pair.compareTo(pair), is(0));
    }

    @Test
    @DataProvider(value = {
            "null       |   null        |   notused     |   notused     |   0",
            "foo        |   null        |   notused     |   notused     |   1",
            "null       |   foo         |   notused     |   notused     |   -1",
            "aaa        |   aaa         |   notused     |   notused     |   0",
            "aaa        |   bbb         |   notused     |   notused     |   -1",
            "bbb        |   aaa         |   notused     |   notused     |   1",
            "notused    |   notused     |   null        |   null        |   0",
            "notused    |   notused     |   foo         |   null        |   1",
            "notused    |   notused     |   null        |   foo         |   -1",
            "notused    |   notused     |   aaa         |   aaa         |   0",
            "notused    |   notused     |   aaa         |   bbb         |   -1",
            "notused    |   notused     |   bbb         |   aaa         |   1",
    }, splitBy = "\\|")
    public void compareTo_returns_expected_value_for_known_data(String p1left, String p2left, String p1right, String p2right, int expected) {
        // given
        Pair<String, String> p1 = PairForTests.of(p1left, p1right);
        Pair<String, String> p2 = PairForTests.of(p2left, p2right);

        // expect
        assertThat(p1.compareTo(p2), is(expected));
    }

    @Test
    public void equals_returns_true_if_compared_to_same_instance() {
        // given
        Pair<String, String> pair = PairForTests.of("foo", "bar");

        // expect
        //noinspection EqualsWithItself
        assertThat(pair.equals(pair), is(true));
    }

    @Test
    public void equals_returns_false_if_compared_to_object_that_is_not_a_MapEntry() {
        // given
        Pair<String, String> pair = PairForTests.of("foo", "bar");

        // expect
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(pair.equals("foobar"), is(false));
    }

    @Test
    @DataProvider(value = {
            "foo    |   foo     |   bar     |   bar     |   true",
            "foo    |   foo2    |   bar     |   bar     |   false",
            "foo2   |   foo     |   bar     |   bar     |   false",
            "foo    |   foo     |   bar     |   bar2    |   false",
            "foo    |   foo     |   bar2    |   bar     |   false",
    }, splitBy = "\\|")
    public void equals_returns_expected_value_for_known_data(String p1left, String p2left, String p1right, String p2right, boolean expected) {
        // given
        Pair<String, String> p1 = PairForTests.of(p1left, p1right);
        Pair<String, String> p2 = PairForTests.of(p2left, p2right);

        // expect
        assertThat(p1.equals(p2), is(expected));
    }

    @Test
    @DataProvider(value = {
            "null   |   null",
            "null   |   foo",
            "foo    |   null",
            "foo    |   bar",
    }, splitBy = "\\|")
    public void hashCode_returns_expected_value_for_known_data(String left, String right) {
        // given
        Pair<String, String> pair = PairForTests.of(left, right);
        int keyHash = (pair.getKey() == null) ? 0 : pair.getKey().hashCode();
        int valueHash = (pair.getValue() == null) ? 0 : pair.getValue().hashCode();

        // expect
        assertThat(pair.hashCode(), is(keyHash ^ valueHash));
    }

    @Test
    public void toString_works_as_expected() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();
        Pair<String, String> pair = PairForTests.of(left, right);

        // expect
        assertThat(pair.toString(), is("(" + left + "," + right + ")"));
    }

    @Test
    public void toString_with_format_works_as_expected() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();
        Pair<String, String> pair = PairForTests.of(left, right);
        String format = "{%1$s|%2$s}";

        // expect
        assertThat(pair.toString(format), is("{" + left + "|" + right + "}"));
    }

    @Test
    public void pair_dot_of_creates_ImmutablePair_with_given_values() {
        // given
        String left = UUID.randomUUID().toString();
        String right = UUID.randomUUID().toString();

        // when
        Pair<String, String> pair = Pair.of(left, right);

        // then
        assertThat(pair, instanceOf(ImmutablePair.class));
        assertThat(pair.getLeft(), is(left));
        assertThat(pair.getRight(), is(right));
    }
}