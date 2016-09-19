package com.nike.backstopper.apierror;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import static java.util.Collections.singletonList;

/**
 * A {@link java.util.SortedSet} that uses a built-in {@link ApiErrorComparator} to compare the {@link ApiError}
 * instances to each other. This should be used any time a {@code SortedSet<ApiError>} is called for.
 *
 * @author Nic Munroe
 */
public class SortedApiErrorSet extends TreeSet<ApiError> {

    private static final Comparator<ApiError> API_ERROR_COMPARATOR = new ApiErrorComparator();

    public SortedApiErrorSet() {
        super(API_ERROR_COMPARATOR);
    }

    public SortedApiErrorSet(Collection<ApiError> values) {
        super(API_ERROR_COMPARATOR);
        addAll(values);
    }

    /**
     * @return The given single {@link ApiError} after it has been wrapped in a new {@link SortedApiErrorSet}.
     */
    public static SortedApiErrorSet singletonSortedSetOf(ApiError apiError) {
        return new SortedApiErrorSet(singletonList(apiError));
    }
}
