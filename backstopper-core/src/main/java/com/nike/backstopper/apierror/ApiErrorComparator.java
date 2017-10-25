package com.nike.backstopper.apierror;

import com.nike.backstopper.util.ApiErrorUtil;

import java.util.Comparator;

import static com.nike.backstopper.util.ApiErrorUtil.generateApiErrorHashCode;
import static com.nike.backstopper.util.ApiErrorUtil.isApiErrorEqual;

/**
 * A comparator that knows how to compare {@link ApiError} instances by {@link ApiError#getName()} first,
 * then by {@link ApiError#getErrorCode()}, and finally by everything else by comparing hashcodes using
 * {@link ApiErrorUtil#generateApiErrorHashCode(ApiError)}.
 * <p>
 * <p>Note that this means two {@link ApiError}s that are identical other than metadata will be considered
 * different by this comparator.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ApiErrorComparator implements Comparator<ApiError> {

    @Override
    public int compare(ApiError o1, ApiError o2) {
        // Use Objects.equals to account for both being null and/or allow impls to specify custom equality logic.
        if (isApiErrorEqual(o1, o2)) {
            return 0;
        }

        // They're not *both* null, but *one* of them might still be null.
        if (o1 == null) {
            return -1;
        }

        if (o2 == null) {
            return 1;
        }

        // Null checks are now out of the way - both are non-null. Since the name should be unique we can just use that
        //      for most use cases.
        int nameComparison = o1.getName().compareTo(o2.getName());
        if (nameComparison != 0)
            return nameComparison;

        // compare error codes after name if names are equal
        int errorCodeComparison = o1.getErrorCode().compareTo(o2.getErrorCode());
        if (errorCodeComparison != 0)
            return errorCodeComparison;

        // At this point we just need something deterministic to compare that will always end up with the same result.
        return Integer.compare(generateApiErrorHashCode(o1), generateApiErrorHashCode(o2));
    }
}
