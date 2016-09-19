package com.nike.backstopper.exception;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A runtime exception representing a <b>SERVERSIDE</b> JSR 303 validation failure (i.e. a validation error with
 * communication between serverside dependencies; something that the client has no control over and likely cannot fix
 * by changing what they send us).
 *
 * <p>If a controller (or its delegate) throws one of these then it will be caught by a
 * {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} via
 * {@link com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener} and
 * turned into a {@link ProjectApiErrors#getServersideValidationApiError()} for the client (usually a 500 error with
 * no details on exactly what went wrong - no information leak about the internal server error),
 * along with logging as much data about this error as possible for debugging purposes.
 *
 * <p><b>NOTE:</b> As mentioned previously this exception is for validation errors with the communication between
 * serverside dependencies that the client likely cannot do anything about. If you have a validation error with data
 * sent by the client where they violated contracts and therefore the problem is the client's and they need to fix it,
 * then you should use {@link com.nike.backstopper.exception.ClientDataValidationError} instead.
 *
 * @author Nic Munroe
 */
public class ServersideValidationError extends RuntimeException {
    private final Object objectThatFailedValidation;
    private final Set<ConstraintViolation<Object>> violations;

    public ServersideValidationError(Object objectThatFailedValidation, Set<ConstraintViolation<Object>> violations) {
        this.objectThatFailedValidation = objectThatFailedValidation;
        this.violations = violations;
    }

    public Object getObjectThatFailedValidation() {
        return objectThatFailedValidation;
    }

    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }
}
