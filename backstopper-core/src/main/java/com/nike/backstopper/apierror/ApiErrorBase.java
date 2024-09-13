package com.nike.backstopper.apierror;

import com.nike.backstopper.util.ApiErrorUtil;

import java.util.Collections;
import java.util.Map;

/**
 * A base class that can handle the requirements of {@link ApiError}.
 *
 * <p>It is very helpful to have a project's series of {@link ApiError}s defined as an enum, but you don't want to have
 * to re-implement {@link ApiError}'s interface every time. A common solution is to have each enum case keep an instance
 * of this base class as a private field and delegate the {@link ApiError} interface calls to the base class. See
 * {@link com.nike.backstopper.apierror.sample.SampleCoreApiError} for an example of this pattern.
 *
 * @author Nic Munroe
 */
public class ApiErrorBase implements ApiError {

    private final String name;
    private final String errorCode;
    private final String message;
    private final int httpStatusCode;
    private final Map<String, Object> metadata;

    public ApiErrorBase(String name, String errorCode, String message, int httpStatusCode,
                        Map<String, Object> metadata) {
        if (name == null) {
            throw new IllegalArgumentException("ApiError name cannot be null");
        }
        if (errorCode == null) {
            throw new IllegalArgumentException("ApiError errorCode cannot be null");
        }
        this.name = name;
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }

        this.metadata = (metadata.isEmpty())
                        ? Collections.emptyMap()
                        : Map.copyOf(metadata);
    }

    public ApiErrorBase(String name, int errorCode, String message, int httpStatusCode, Map<String, Object> metadata) {
        this(name, String.valueOf(errorCode), message, httpStatusCode, metadata);
    }

    public ApiErrorBase(String name, String errorCode, String message, int httpStatusCode) {
        this(name, errorCode, message, httpStatusCode, null);
    }

    public ApiErrorBase(String name, int errorCode, String message, int httpStatusCode) {
        this(name, errorCode, message, httpStatusCode, null);
    }

    public ApiErrorBase(ApiError mirror) {
        this(mirror, mirror.getName());
    }

    public ApiErrorBase(ApiError mirror, String newName) {
        this(newName, mirror.getErrorCode(), mirror.getMessage(), mirror.getHttpStatusCode(), mirror.getMetadata());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return ApiErrorUtil.isApiErrorEqual(this, o);
    }

    @Override
    public int hashCode() {
        return ApiErrorUtil.generateApiErrorHashCode(this);
    }

}
