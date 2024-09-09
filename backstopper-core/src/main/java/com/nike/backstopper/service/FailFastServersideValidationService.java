package com.nike.backstopper.service;

import com.nike.backstopper.exception.ServersideValidationError;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Helper service that provides a method for fail-fast JSR 303 validation of serverside objects (e.g. objects received
 * by the server from downstream calls to another service). See {@link #validateObjectFailFast(Object)} and
 * {@link ServersideValidationError} for more info.
 *
 * <p>NOTE: The constructor requires a non-null {@link Validator}. This can be problematic when a dependency injection
 * system automatically picks up on the dependency injection annotations on this class and tries to eagerly create an
 * instance, but your project does not have a JSR 303 validation implementation on your classpath and you don't actually
 * need this class for anything. In that case you can wire up {@link NoOpJsr303Validator#SINGLETON_IMPL} as the
 * {@link Validator} for your dependency injection system which will cause this class to be injected with
 * a no-op impl, thus satisfying the constructor requirements without pulling in a JSR 303 implementation dependency
 * into your project.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
public class FailFastServersideValidationService {

    private final Validator validator;

    @Inject
    public FailFastServersideValidationService(Validator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("Validator cannot be null");
        }

        this.validator = validator;
    }

    /**
     * Performs JSR 303 validation of the given object, and throws a {@link ServersideValidationError} if any constraint
     * violations are found. If this method returns without throwing an exception then the object successfully passed
     * validation.
     */
    @SuppressWarnings("WeakerAccess")
    public void validateObjectFailFast(Object validateMe) {
        // Check the object for JSR 303 validation errors.
        Set<ConstraintViolation<Object>> violations = validator.validate(validateMe);

        // If it came back clean we're done - just return.
        if (violations.isEmpty()) {
            return;
        }

        // If we reach here then it didn't come back clean. We have at least one validation error.
        throw new ServersideValidationError(validateMe, violations);
    }
}
