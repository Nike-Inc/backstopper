package com.nike.backstopper.apierror;

import java.util.Comparator;
import java.util.Objects;

/**
 * A comparator that knows how to compare {@link ApiError} instances by {@link ApiError#getName()} first, and then by
 * {@link ApiError#getMetadata()} to allow for multiple same-named errors that have different metadata.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ApiErrorComparator implements Comparator<ApiError> {

    @Override
    public int compare(ApiError o1, ApiError o2) {
        // Use Objects.equals to account for both being null and/or allow impls to specify custom equality logic.
        if (Objects.equals(o1, o2)) {
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

        // Name is the same, so they should be the same error. We do allow same-error-but-different-metadata through
        //      as distinct errors, so the final comparison is the metadata map.
        boolean metadataEqual = o1.getMetadata().equals(o2.getMetadata());

        if (metadataEqual)
            return 0;

        // Metadata are not equal, so we consider these separate errors. At this point we just need something
        //      deterministic to compare that will always end up with the same result.
        return Integer.compare(o1.hashCode(), o2.hashCode());
    }
}
