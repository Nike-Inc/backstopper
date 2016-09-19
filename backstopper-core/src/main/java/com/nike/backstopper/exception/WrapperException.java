package com.nike.backstopper.exception;

/**
 * Simple wrapper exception that you can use when you want the error handling system to handle the
 * {@link WrapperException#getCause()} of this instance rather than this instance itself, but still log the entire
 * stack trace (including this instance). This is often necessary in asynchronous scenarios where you want to add
 * stack trace info for the logs but don't want to obscure the true cause of the error.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class WrapperException extends RuntimeException {

    public WrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrapperException(Throwable cause) {
        super(cause);
    }
}
