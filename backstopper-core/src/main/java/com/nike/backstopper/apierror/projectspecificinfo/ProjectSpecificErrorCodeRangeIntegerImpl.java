package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.range.IntegerRange;

import java.util.Collections;
import java.util.List;

/**
 * Simple implementation of {@link ProjectSpecificErrorCodeRange} that uses {@link IntegerRange}s to determine
 * the result of calling {@link #isInRange(ApiError)}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ProjectSpecificErrorCodeRangeIntegerImpl implements ProjectSpecificErrorCodeRange {

    protected final String name;
    protected final List<IntegerRange> ranges;

    /**
     * Creates a new instance with the given args - this is a shortcut for calling
     * {@link #ProjectSpecificErrorCodeRangeIntegerImpl(List, String)} and passing in a singleton list containing
     * an {@link IntegerRange} built from the given lower and upper bounds.
     *
     * @param lowerRangeInclusive The lower bound (inclusive) that should be considered in-range for this instance.
     * @param upperRangeInclusive The upper bound (inclusive) that should be considered in-range for this instance.
     * @param name The name of this instance - will be returned by {@link #getName()}.
     */
    public ProjectSpecificErrorCodeRangeIntegerImpl(int lowerRangeInclusive, int upperRangeInclusive, String name) {
        this(Collections.singletonList(IntegerRange.of(lowerRangeInclusive, upperRangeInclusive)), name);
    }

    /**
     * Creates a new instance with the given args.
     *
     * @param ranges The list of ranges that should be considered in-range for this instance.
     * @param name The name of this instance - will be returned by {@link #getName()}.
     */
    public ProjectSpecificErrorCodeRangeIntegerImpl(List<IntegerRange> ranges, String name) {
        this.name = name;
        this.ranges = ranges;
    }

    /**
     * @return true if the given error's {@link ApiError#getErrorCode()} falls within one of the this instance's
     *          {@link #ranges} IntegerRange objects. In other words this returns true if and only if
     *          {@link IntegerRange#isInRange(int)} returns true for one of the items in this instance's
     *          {@link #ranges} list.
     */
    @Override
    public boolean isInRange(ApiError error) {
        for (IntegerRange range : ranges) {
            if (range.isInRange(error.getErrorCode()))
                return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return name;
    }
}
