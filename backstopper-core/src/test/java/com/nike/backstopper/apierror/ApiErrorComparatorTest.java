package com.nike.backstopper.apierror;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link com.nike.backstopper.apierror.ApiErrorComparator}
 */
public class ApiErrorComparatorTest {
    ApiErrorComparator comparator = new ApiErrorComparator();

    @Test
    public void shouldReturn0ForReferenceEquality() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(mockApiError, mockApiError), is(0));
    }

    @Test
    public void shouldReturnNeg1WhenFirstArgIsNull() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(null, mockApiError), is(-1));
    }

    @Test
    public void shouldReturn1WhenSecondArgIsNull() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(mockApiError, null), is(1));
    }

    @Test
    public void shouldUseNameComparisonWhenArgsAreValid() {
        ApiError mockApiError1 = mock(ApiError.class);
        ApiError mockApiError2 = mock(ApiError.class);

        String mockApiError1Name = "asdf";
        String mockApiError2Name = "qwer";
        doReturn(mockApiError1Name).when(mockApiError1).getName();
        doReturn(mockApiError2Name).when(mockApiError2).getName();

        assertThat(comparator.compare(mockApiError1, mockApiError2), is(mockApiError1Name.compareTo(mockApiError2Name)));
    }
}