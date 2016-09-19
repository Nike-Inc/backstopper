package com.nike.backstopper.apierror;

import java.util.Comparator;

/**
 * A comparator that knows how to compare {@link ApiError} instances by {@link ApiError#getName()}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ApiErrorComparator implements Comparator<ApiError> {

    @Override
    public int compare(ApiError o1, ApiError o2) {
        // Handles the trivial case of true equality. Also handles the case of both being null.
        if (o1 == o2) {
            return 0;
        }

        if (o1 == null) {
            return -1;
        }

        if (o2 == null) {
            return 1;
        }

        // Null checks are now out of the way - both are non-null. Since the name should be unique we can just use that.
        return o1.getName().compareTo(o2.getName());
    }
}
