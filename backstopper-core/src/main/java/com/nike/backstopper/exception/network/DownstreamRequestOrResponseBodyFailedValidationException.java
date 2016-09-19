package com.nike.backstopper.exception.network;

/**
 * Exception for when a downstream server's request or response body failed validation (e.g. JSR 303 validation).
 * Since this usually happens because the request/response body was passed to
 * {@link com.nike.backstopper.service.FailFastServersideValidationService} for validation and failed,
 * the {@link #getCause()} is likely (but not guaranteed) to be a
 * {@link com.nike.backstopper.exception.ServersideValidationError}.
 *
 * @author Nic Munroe
 */
public class DownstreamRequestOrResponseBodyFailedValidationException extends NetworkExceptionBase {
    public DownstreamRequestOrResponseBodyFailedValidationException(Throwable cause, String connectionType) {
        super(cause, connectionType);
    }
}
