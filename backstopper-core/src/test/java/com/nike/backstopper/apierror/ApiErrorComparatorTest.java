package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link com.nike.backstopper.apierror.ApiErrorComparator}
 */
public class ApiErrorComparatorTest {
    private ApiErrorComparator comparator = new ApiErrorComparator();

    @Test
    public void should_return_0_for_reference_equality() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(mockApiError, mockApiError)).isEqualTo(0);
    }

    @Test
    public void should_return_0_for_both_null() {
        assertThat(comparator.compare(null, null)).isEqualTo(0);
    }

    @Test
    public void should_return_neg_1_when_first_arg_is_null() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(null, mockApiError)).isEqualTo(-1);
    }

    @Test
    public void should_return_1_when_second_arg_is_null() {
        ApiError mockApiError = mock(ApiError.class);
        assertThat(comparator.compare(mockApiError, null)).isEqualTo(1);
    }

    @Test
    public void should_use_name_comparison_when_args_are_valid() {
        ApiError mockApiError1 = mock(ApiError.class);
        ApiError mockApiError2 = mock(ApiError.class);

        String mockApiError1Name = "asdf";
        String mockApiError2Name = "qwer";
        doReturn(mockApiError1Name).when(mockApiError1).getName();
        doReturn(mockApiError2Name).when(mockApiError2).getName();

        assertThat(comparator.compare(mockApiError1, mockApiError2)).isEqualTo(mockApiError1Name.compareTo(mockApiError2Name));
    }

    @Test
    public void should_return_0_if_names_and_metadata_are_equal() {
        // given
        ApiError mockApiError1 = mock(ApiError.class);
        ApiError mockApiError2 = mock(ApiError.class);

        String name = UUID.randomUUID().toString();
        doReturn(name).when(mockApiError1).getName();
        doReturn(name).when(mockApiError2).getName();

        Map<String, Object> metadata1 = MapBuilder.builder("key1", (Object)"value1")
                                                  .put("key2", "value2")
                                                  .build();
        Map<String, Object> metadata2 = new HashMap<>(metadata1);

        doReturn(metadata1).when(mockApiError1).getMetadata();
        doReturn(metadata2).when(mockApiError2).getMetadata();

        // when
        int result = comparator.compare(mockApiError1, mockApiError2);

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    public void should_use_hashCode_comparison_when_names_are_equal_and_metadata_is_different() {
        // given
        ApiError apiError = new ApiErrorBase(UUID.randomUUID().toString(), 42, "foo", 400);
        ApiError errorWithMetadata = new ApiErrorWithMetadata(apiError, Pair.of("bar", (Object)UUID.randomUUID().toString()));
        assertThat(apiError.hashCode()).isNotEqualTo(errorWithMetadata.hashCode());

        // when
        int result = comparator.compare(apiError, errorWithMetadata);

        // then
        assertThat(result).isEqualTo(Integer.compare(apiError.hashCode(), errorWithMetadata.hashCode()));
    }
}