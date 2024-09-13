package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable DTO for a single specific error to be returned to the client. Used by {@link DefaultErrorContractDTO}. This
 * represents the recommended default format for a specific error returned to the client and mirrors the info available
 * in {@link ApiError} making the errors your app throws and what shows up to the client in the response closely
 * related and easy to reason about, but you are not required to use it - ultimately you control what gets returned
 * to the client based on what
 * {@code ApiExceptionHandlerBase.prepareFrameworkRepresentation()} or
 * {@code UnhandledExceptionHandlerBase.prepareFrameworkRepresentation()} returns.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class DefaultErrorDTO implements Serializable {
    /**
     * The application-specific code associated with this error. This is *not* an HTTP Status Code.
     */
    public final String code;
    /**
     * The human-readable message associated with this error.
     */
    public final String message;
    /**
     * Arbitrary metadata about the error that can be used to add extra info to help clients understand more about what
     * went wrong. For example, if this error represented a "missing expected content" type of error but you wanted to
     * indicate which field was missing you could add a "missing_field" -> "foo" mapping to the metadata map.
     * {@link ApiErrorWithMetadata} exists for wrapping any pre-existing {@link ApiError} in order to add metadata to
     * it, which could then be passed into the {@link DefaultErrorDTO#DefaultErrorDTO(ApiError)} constructor.
     */
    public final Map<String, Object> metadata;

    // Here for deserialization support only - usage in real code should involve one of the other constructors since
    //      this class is immutable
    protected DefaultErrorDTO() {
        this(null, null, null);
    }

    /**
     * Creates a new instance that is a copy of the given instance.
     */
    public DefaultErrorDTO(DefaultErrorDTO copy) {
        this(copy.code, copy.message, copy.metadata);
    }

    /**
     * Creates a new instance with code, message, and metadata pulled from the given {@link ApiError}.
     */
    public DefaultErrorDTO(ApiError error) {
        this(error.getErrorCode(), error.getMessage(), error.getMetadata());
    }

    /**
     * Creates a new instance with the given code (interpreted as a string), message, and metadata.
     */
    public DefaultErrorDTO(int code, String message, Map<String, Object> metadata) {
        this(String.valueOf(code), message, metadata);
    }

    /**
     * Creates a new instance with the given code, message, and metadata.
     */
    public DefaultErrorDTO(String code, String message, Map<String, Object> metadata) {
        this.code = code;
        this.message = message;
        if (metadata == null)
            metadata = Collections.emptyMap();
        this.metadata = (metadata.isEmpty())
                        ? Collections.emptyMap()
                        : Map.copyOf(metadata);
    }
}
