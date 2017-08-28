package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.nike.backstopper.apierror.SortedApiErrorSet.DEFAULT_API_ERROR_COMPARATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link SortedApiErrorSet}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class SortedApiErrorSetTest {

    @Test
    public void default_constructor_uses_ApiErrorComparator() {
        // when
        SortedApiErrorSet set = new SortedApiErrorSet();

        // then
        assertThat(set.comparator())
            .isInstanceOf(ApiErrorComparator.class)
            .isSameAs(DEFAULT_API_ERROR_COMPARATOR);
        assertThat(set).isEmpty();
    }

    private Random random = new Random();
    private ApiError generateRandomApiError() {
        return new ApiErrorBase(UUID.randomUUID().toString(), random.nextInt(),
                                UUID.randomUUID().toString(), random.nextInt());
    }

    @Test
    public void one_arg_constructor_with_values_uses_ApiErrorComparator_and_adds_values() {
        // given
        Collection<ApiError> values = Arrays.asList(
            generateRandomApiError(),
            generateRandomApiError()
        );

        // when
        SortedApiErrorSet set = new SortedApiErrorSet(values);

        // then
        assertThat(set.comparator())
            .isInstanceOf(ApiErrorComparator.class)
            .isSameAs(DEFAULT_API_ERROR_COMPARATOR);
        assertThat(set).containsExactlyInAnyOrder(values.toArray(new ApiError[]{}));
    }

    @Test
    public void one_arg_constructor_with_comparator_uses_supplied_comparator() {
        // given
        Comparator<ApiError> customComparator = mock(Comparator.class);

        // when
        SortedApiErrorSet set = new SortedApiErrorSet(customComparator);

        // then
        assertThat(set.comparator())
            .isSameAs(customComparator)
            .isNotSameAs(DEFAULT_API_ERROR_COMPARATOR);
        assertThat(set).isEmpty();
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void two_arg_constructor_uses_supplied_args(boolean useSameError) {
        // given
        ApiError error1 = generateRandomApiError();
        ApiError error2 = (useSameError) ? error1 : generateRandomApiError();
        Collection<ApiError> values = Arrays.asList(
            error1,
            error2
        );

        Comparator<ApiError> customComparator = new ApiErrorComparator();

        // when
        SortedApiErrorSet set = new SortedApiErrorSet(values, customComparator);

        // then
        assertThat(set.comparator())
            .isSameAs(customComparator)
            .isNotSameAs(DEFAULT_API_ERROR_COMPARATOR);
        if (useSameError) {
            assertThat(set).containsOnly(error1);
        }
        else {
            assertThat(set).containsOnly(error1, error2);
        }
    }

    @Test
    public void singletonSortedSetOf_returns_singleton_set_with_supplied_arg_and_default_comparator() {
        // given
        ApiError error = generateRandomApiError();

        // when
        SortedApiErrorSet result = SortedApiErrorSet.singletonSortedSetOf(error);

        // then
        assertThat(result).containsOnly(error);
        assertThat(result.comparator()).isSameAs(DEFAULT_API_ERROR_COMPARATOR);
    }

    @Test
    public void default_config_supports_multiple_errors_that_differ_only_by_metadata() {
        // given
        ApiError baseError = new ApiErrorBase(UUID.randomUUID().toString(), 42, "foo", 400);
        ApiError errorWithMetadata1 = new ApiErrorWithMetadata(baseError, Pair.of("foo", (Object)"bar"));
        ApiError errorWithMetadata2 = new ApiErrorWithMetadata(baseError, Pair.of("foo", (Object)"notbar"));

        SortedApiErrorSet set = new SortedApiErrorSet();
        assertThat(set).isEmpty();

        // when
        set.add(errorWithMetadata1);
        set.add(errorWithMetadata2);

        // then
        assertThat(set).hasSize(2);
        assertThat(set).containsExactlyInAnyOrder(errorWithMetadata1, errorWithMetadata2);
    }

    @Test
    public void default_config_filters_out_multiple_errors_that_have_same_name_and_metadata() {
        // given
        String name = UUID.randomUUID().toString();
        Map<String, Object> metadata =
            MapBuilder.builder(UUID.randomUUID().toString(), (Object)UUID.randomUUID().toString()).build();

        ApiError baseError1 = new ApiErrorBase(name, 42, "foo", 400);
        ApiError baseError2 = new ApiErrorBase(name, 42, "foo", 400);

        ApiError errorWithMetadata1 = new ApiErrorWithMetadata(baseError1, metadata);
        ApiError errorWithMetadata2 = new ApiErrorWithMetadata(baseError2, new HashMap<>(metadata));

        SortedApiErrorSet set = new SortedApiErrorSet();
        assertThat(set).isEmpty();

        // when
        set.add(errorWithMetadata1);
        set.add(errorWithMetadata2);

        // then
        assertThat(set).hasSize(1);
        ApiError remainingError = set.iterator().next();
        assertThat(remainingError.getName()).isEqualTo(name);
        assertThat(remainingError.getMetadata()).isEqualTo(metadata);
        assertThat(
            (remainingError == errorWithMetadata1) || (remainingError == errorWithMetadata2)
        ).isTrue();
    }
}