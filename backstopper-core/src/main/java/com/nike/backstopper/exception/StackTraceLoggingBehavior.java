package com.nike.backstopper.exception;

/**
 * An enum representing the options available for whether the stack trace is logged for a given exception caught and
 * handled by Backstopper. {@link ApiException} includes this as an optional field you can set which Backstopper will
 * honor when handling the {@link ApiException}.
 */
public enum StackTraceLoggingBehavior {
    /**
     * This option forces Backstopper to log the stack trace of the exception, even if it would normally not log the
     * stack trace (e.g. if the exception represents a 4xx error).
     */
    FORCE_STACK_TRACE,
    /**
     * This option forces Backstopper to *not* log the stack trace of the exception, even if it would normally log the
     * stack trace (e.g. if the exception represents a 5xx error).
     */
    FORCE_NO_STACK_TRACE,
    /**
     * This option lets Backstopper decide whether or not the stack trace of the exception should be logged. This
     * usually means the stack trace will be logged for an exception representing a 5xx error, and no stack trace for
     * exceptions representing a 4xx error.
     */
    DEFER_TO_DEFAULT_BEHAVIOR
}
