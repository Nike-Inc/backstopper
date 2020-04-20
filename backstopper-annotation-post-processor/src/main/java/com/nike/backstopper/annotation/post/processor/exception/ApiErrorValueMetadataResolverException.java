package com.nike.backstopper.annotation.post.processor.exception;

import com.nike.backstopper.model.ApiErrorValueMetadata;

/**
 * This exception is used to signal {@link ApiErrorValueMetadata} resolution issues.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataResolverException extends RuntimeException {

    public ApiErrorValueMetadataResolverException(String message, Throwable cause) {
        super(message, cause);
    }

}
