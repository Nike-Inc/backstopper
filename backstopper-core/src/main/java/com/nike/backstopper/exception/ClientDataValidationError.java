package com.nike.backstopper.exception;

import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * A runtime exception representing a <b>CLIENT DATA</b> JSR 303 validation failure (i.e. a validation error with data
 * passed to the server from the client where the client violated a contract; something that the client has control
 * over and can fix by changing what they send the server).
 *
 * <p>If a controller (or its delegate) throws one of these then it will be caught by a
 * {@link com.nike.backstopper.handler.ApiExceptionHandlerBase} via
 * {@link com.nike.backstopper.handler.listener.impl.ClientDataValidationErrorHandlerListener} and
 * turned into the appropriate set of {@link com.nike.backstopper.apierror.ApiError}s for the client, along with logging
 * as much data about this error as possible for debugging purposes.
 *
 * <p><b>NOTE:</b> As mentioned previously this exception is for validation errors where the client sending the data
 * violated a contract and can fix the problem themselves. If you have a validation error with data sent between this
 * app and a downstream service where the client has no control, then you should use
 * {@link com.nike.backstopper.exception.ServersideValidationError} instead.
 *
 * @author Nic Munroe
 */
public class ClientDataValidationError extends RuntimeException {
    private final List<Object> objectsThatFailedValidation;
    private final List<ConstraintViolation<Object>> violations;
    private final Class<?>[] validationGroups;

    public ClientDataValidationError(List<Object> objectsThatFailedValidation,
                                     List<ConstraintViolation<Object>> violations, Class<?>[] validationGroups) {
        this.objectsThatFailedValidation = objectsThatFailedValidation;
        this.violations = violations;
        this.validationGroups = validationGroups;
    }

    /**
     * @return The list of objects that failed validation. Each of these objects has at least one violation in
     *          {@link #getViolations()}.
     */
    public List<Object> getObjectsThatFailedValidation() {
        return objectsThatFailedValidation;
    }

    /**
     * @return The list of constraint violations that were detected for {@link #getObjectsThatFailedValidation()}.
     */
    public List<ConstraintViolation<Object>> getViolations() {
        return violations;
    }

    /**
     * @return The validation groups that were used to do the validation, if any. This may be null or empty; if this is
     *          null/empty it means the {@link javax.validation.groups.Default} validation group was used as per
     *          {@link javax.validation.Validator#validate(Object, Class[])} (i.e. no groups were passed in to that
     *          method).
     */
    public Class<?>[] getValidationGroups() {
        return validationGroups;
    }
}
