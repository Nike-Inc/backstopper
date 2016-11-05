package com.nike.backstopper.apierror;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ApiError} that takes a base delegate {@link ApiError} and some extra metadata. When you call
 * {@link #getMetadata()} on this instance you'll get a combination of the original delegate's metadata and the
 * additional metadata you passed into the constructor. All other {@link ApiError} method calls will fall straight
 * through to the delegate.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class ApiErrorWithMetadata implements ApiError {

    protected final ApiError delegate;
    protected final Map<String, Object> comboMetadata;

    public ApiErrorWithMetadata(ApiError delegate, Map<String, Object> extraMetadata) {
        if (delegate == null)
            throw new IllegalArgumentException("ApiError delegate cannot be null");

        this.delegate = delegate;

        Map<String, Object> delegateMetadata = delegate.getMetadata();
        if (delegateMetadata == null)
            delegateMetadata = Collections.emptyMap();

        if (extraMetadata == null)
            extraMetadata = Collections.emptyMap();

        Map<String, Object> unprotectedCombo = new HashMap<>(delegateMetadata);
        unprotectedCombo.putAll(extraMetadata);

        this.comboMetadata = Collections.unmodifiableMap(unprotectedCombo);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getErrorCode() {
        return delegate.getErrorCode();
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return this.comboMetadata;
    }

    @Override
    public int getHttpStatusCode() {
        return delegate.getHttpStatusCode();
    }
}
