package com.nike.internal.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link StringUtils}
 */
@RunWith(DataProviderRunner.class)
public class StringUtilsTest {

    @Test
    @DataProvider(value = {
            "foo        |   bar         |   ,           |   foo,bar",
            "whee       |   42          |   !           |   whee!42",
            "test       |   things      |   _allthe_    |   test_allthe_things",
            "null       |   null        |   ,           |   null,null",
    }, splitBy = "\\|")
    public void join_without_prefix_and_suffix_works_as_expected_for_known_data(String item1, String item2, String delimiter, String expected) {
        // given
        List<String> listToJoin = Arrays.asList(item1, item2);

        // expect
        assertThat(StringUtils.join(listToJoin, delimiter), is(expected));
    }

    @Test
    public void join_without_prefix_returns_empty_string_if_passed_empty_collection() {
        // expect
        assertThat(StringUtils.join(Collections.emptyList(), ","), is(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void join_without_prefix_throws_IllegalArgumentException_if_passed_null_collection() {
        // expect
        StringUtils.join(null, ",");
    }

    @Test(expected = IllegalArgumentException.class)
    public void join_without_prefix_throws_IllegalArgumentException_if_passed_null_delimiter() {
        // expect
        StringUtils.join(Arrays.asList("foo", "bar"), null);
    }

    @Test
    @DataProvider(value = {
            "foo        |   bar         |   ,           |   null    |   null    |   foo,bar",
            "whee       |   42          |   !           |   null    |   null    |   whee!42",
            "test       |   things      |   _allthe_    |   null    |   null    |   test_allthe_things",
            "foo        |   bar         |   ,           |   [       |   ]       |   [foo,bar]",
            "foo        |   bar         |   ,           |           |   _suffix |   foo,bar_suffix",
            "foo        |   bar         |   ,           |   null    |   _suffix |   foo,bar_suffix",
            "foo        |   bar         |   ,           |   prefix_ |           |   prefix_foo,bar",
            "foo        |   bar         |   ,           |   prefix_ |   null    |   prefix_foo,bar",
            "null       |   null        |   ,           |   null    |   null    |   null,null",
    }, splitBy = "\\|")
    public void join_with_prefix_and_suffix_works_as_expected_for_known_data(String item1, String item2, String delimiter, String prefix, String suffix, String expected) {
        // given
        List<String> listToJoin = Arrays.asList(item1, item2);

        // expect
        assertThat(StringUtils.join(listToJoin, delimiter, prefix, suffix), is(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void join_with_prefix_throws_IllegalArgumentException_if_passed_null_collection() {
        // expect
        StringUtils.join(null, ",", "[", "]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void join_with_prefix_throws_IllegalArgumentException_if_passed_null_delimiter() {
        // expect
        StringUtils.join(Arrays.asList("foo", "bar"), null, "[", "]");
    }

    @Test
    public void join_with_prefix_returns_prefix_and_suffix_only_if_passed_empty_collection() {
        // expect
        assertThat(StringUtils.join(Collections.emptyList(), ",", "[", "]"), is("[]"));
    }

    @DataProvider
    public static Object[][] dataProviderForIsEmpty() {
        return new Object[][] {
                { null, true },
                { "", true },
                { " ", false },
                { "bob", false },
                { "  bob  ", false }
        };
    }

    @Test
    @UseDataProvider("dataProviderForIsEmpty")
    public void isEmpty_works_as_expected_for_known_data(String value, boolean expected) {
        // expect
        assertThat(StringUtils.isEmpty(value), is(expected));
    }

    @DataProvider
    public static Object[][] dataProviderForIsNotEmpty() {
        return new Object[][] {
                { null, false },
                { "", false },
                { " ", true },
                { "bob", true },
                { "  bob  ", true }
        };
    }

    @Test
    @UseDataProvider("dataProviderForIsNotEmpty")
    public void isNotEmpty_works_as_expected_for_known_data(String value, boolean expected) {
        // expect
        assertThat(StringUtils.isNotEmpty(value), is(expected));
    }

    @DataProvider
    public static Object[][] dataProviderForIsBlank() {
        return new Object[][] {
                { null, true },
                { "", true },
                { " \t\r\n ", true },
                { "bob", false },
                { "  bob  ", false }
        };
    }

    @Test
    @UseDataProvider("dataProviderForIsBlank")
    public void isBlank_works_as_expected_for_known_data(String value, boolean expected) {
        // expect
        assertThat(StringUtils.isBlank(value), is(expected));
    }

    @DataProvider
    public static Object[][] dataProviderForIsNotBlank() {
        return new Object[][] {
                { null, false },
                { "", false },
                { " \t\r\n ", false },
                { "bob", true },
                { "  bob  ", true }
        };
    }

    @Test
    @UseDataProvider("dataProviderForIsNotBlank")
    public void isNotBlank_works_as_expected_for_known_data(String value, boolean expected) {
        // expect
        assertThat(StringUtils.isNotBlank(value), is(expected));
    }
}