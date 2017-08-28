package com.nike.backstopper.apierror;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import static java.util.Collections.singletonList;

/**
 * A {@link java.util.SortedSet} that uses a built-in {@link ApiErrorComparator} to compare the {@link ApiError}
 * instances to each other. This should be used any time a {@code SortedSet<ApiError>} is called for.
 *
 * <p>There are some constructors to allow you to pass in a custom comparator rather than the default (a
 * {@link ApiErrorComparator}) - use them if you're sure the default behavior does not work for your use case.
 *
 * @author Nic Munroe
 */
public class SortedApiErrorSet extends TreeSet<ApiError> {

    protected static final Comparator<ApiError> DEFAULT_API_ERROR_COMPARATOR = new ApiErrorComparator();

    public SortedApiErrorSet() {
        this(DEFAULT_API_ERROR_COMPARATOR);
    }

    public SortedApiErrorSet(Collection<ApiError> values) {
        this(DEFAULT_API_ERROR_COMPARATOR);
        addAll(values);
    }
    
    public SortedApiErrorSet(Comparator<ApiError> customComparator) {
        super(customComparator);
    }

    public SortedApiErrorSet(Collection<ApiError> values, Comparator<ApiError> customComparator) {
        this(customComparator);
        addAll(values);
    }

    /**
     * @return The given single {@link ApiError} after it has been wrapped in a new {@link SortedApiErrorSet}.
     */
    public static SortedApiErrorSet singletonSortedSetOf(ApiError apiError) {
        return new SortedApiErrorSet(singletonList(apiError));
    }
}
