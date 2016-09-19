package com.nike.backstopper.handler;

/**
 * Typed exception used by {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} to indicate
 * that some unexpected (and major) error occurred while handling an exception. Likely indicates a bug in the exception
 * handler that needs to be fixed.
 *
 * @author Nic Munroe
 */
public class UnexpectedMajorExceptionHandlingError extends Exception {
    @SuppressWarnings("WeakerAccess")
    public UnexpectedMajorExceptionHandlingError(String message, Throwable cause) {
        super(message, cause);
    }
}
