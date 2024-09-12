package com.nike.backstopper.apierror.projectspecificinfo.range;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link IntegerRange}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class IntegerRangeTest {

    private final IntegerRange rangeOfOneToFour = IntegerRange.of(1, 4);

    @Test
    public void constructor_throws_exception_if_upper_range_less_than_lower_range() {
        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                IntegerRange.of(5, 4);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @DataProvider(value = {
        "0  | false",
        "1  | true",
        "2  | true",
        "4  | true",
        "5  | false",
    }, splitBy = "\\|")
    @Test
    public void isInRange_int_arg_works_as_expected(int arg, boolean expectedResult) {
        // expect
        assertThat(rangeOfOneToFour.isInRange(arg)).isEqualTo(expectedResult);
    }

    @DataProvider(value = {
        "0  | false",
        "1  | true",
        "2  | true",
        "4  | true",
        "5  | false",
    }, splitBy = "\\|")
    @Test
    public void isInRange_string_arg_works_as_expected(String arg, boolean expectedResult) {
        // expect
        assertThat(rangeOfOneToFour.isInRange(arg)).isEqualTo(expectedResult);
    }

    @Test
    public void isInRange_string_arg_returns_false_if_arg_cannot_be_parsed_to_an_int() {
        // expect
        assertThat(rangeOfOneToFour.isInRange("foo")).isFalse();
    }
}