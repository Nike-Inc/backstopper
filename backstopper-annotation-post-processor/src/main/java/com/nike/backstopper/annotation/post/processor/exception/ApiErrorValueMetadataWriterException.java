package com.nike.backstopper.annotation.post.processor.exception;

import com.nike.backstopper.model.ApiErrorValueMetadata;

/**
 * This exception is used to signal {@link ApiErrorValueMetadata} writing issues.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataWriterException extends RuntimeException {

    public ApiErrorValueMetadataWriterException(String message, Throwable cause) {
        super(message, cause);
    }

}
