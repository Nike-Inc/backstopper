package com.nike.backstopper.service;

import com.nike.backstopper.exception.ClientDataValidationError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Provides methods for performing JSR 303 validation on objects that will throw a {@link ClientDataValidationError} if
 * any constraint violations are found, with the relevant details embedded in the exception.
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
@SuppressWarnings("WeakerAccess")
public class ClientDataValidationService {

    private final Validator validator;

    @Inject
    public ClientDataValidationService(Validator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("Validator cannot be null");
        }

        this.validator = validator;
    }

    /**
     * Performs JSR 303 validation of the given objects (using the default validation group), and throws a
     * {@link ClientDataValidationError} if any constraint violations are found. If this method returns without throwing
     * an exception then the object successfully passed validation. If you pass in null then this method will return
     * without doing anything (i.e. it is safe to pass in null).
     */
    public void validateObjectsFailFast(Object... validateTheseObjects) {
        validateObjectsWithGroupsFailFast((Class<?>[]) null, validateTheseObjects);
    }

    /**
     * Performs JSR 303 validation of the given objects for the given group, and throws a
     * {@link ClientDataValidationError} if any constraint violations are found. If this method returns without throwing
     * an exception then the object successfully passed validation. If you pass in null for the objects then this method
     * will return without doing anything (i.e. it is safe to pass in null). If you pass in null for the group then the
     * Default group will be used (in this case you could have called {@link #validateObjectsFailFast(Object...)} for
     * identical behavior).
     *
     * <p>NOTE: When asking for a specific group to be validated the Default group will not be validated - to validate
     * constraints which are members of the Default group you must pass in {@link jakarta.validation.groups.Default} (in
     * which case you could just call the simpler {@link #validateObjectsFailFast(Object...)} method), or you must pass
     * in a class that extends {@link jakarta.validation.groups.Default}.
     */
    public void validateObjectsWithGroupFailFast(Class<?> group, Object... validateTheseObjects) {
        validateObjectsWithGroupsFailFast(new Class<?>[]{group}, validateTheseObjects);
    }

    /**
     * Performs JSR 303 validation of the given objects for the given groups, and throws a
     * {@link ClientDataValidationError} if any constraint violations are found. If this method returns without throwing
     * an exception then the object successfully passed validation. If you pass in null for the objects then this method
     * will return without doing anything (i.e. it is safe to pass in null). If you pass in null for the groups then the
     * Default group will be used (in this case you could have called {@link #validateObjectsFailFast(Object...)} for
     * identical behavior).
     *
     * <p>NOTE: When asking for specific groups to be validated the Default group will not be validated unless it is
     * included - to validate constraints which are members of the Default group one of the groups you pass in must be
     * {@link jakarta.validation.groups.Default} or must extend it.
     */
    public void validateObjectsWithGroupsFailFast(Collection<Class<?>> groups, Object... validateTheseObjects) {
        Class<?>[] groupsArray =
            (groups == null || groups.isEmpty()) ? null : groups.toArray(new Class<?>[0]);

        validateObjectsWithGroupsFailFast(groupsArray, validateTheseObjects);
    }

    /**
     * Performs JSR 303 validation of the given objects for the given groups, and throws a
     * {@link ClientDataValidationError} if any constraint violations are found. If this method returns without throwing
     * an exception then the object successfully passed validation. If you pass in null for the objects then this method
     * will return without doing anything (i.e. it is safe to pass in null). If you pass in null for the groups then the
     * Default group will be used (in this case you could have called {@link #validateObjectsFailFast(Object...)} for
     * identical behavior).
     *
     * <p>NOTE: When asking for specific groups to be validated the Default group will not be validated unless it is
     * included - to validate constraints which are members of the Default group one of the groups you pass in must be
     * {@link jakarta.validation.groups.Default} or must extend it.
     */
    public void validateObjectsWithGroupsFailFast(Class<?>[] groups, Object... validateTheseObjects) {
        if (validateTheseObjects == null || validateTheseObjects.length == 0) {
            return;
        }

        // Check the object for JSR 303 validation errors.
        List<ConstraintViolation<Object>> violations = new ArrayList<>();
        List<Object> objectsThatFailedValidation = new ArrayList<>();
        for (Object obj : validateTheseObjects) {
            if (obj != null) {
                Set<ConstraintViolation<Object>> objViolations =
                    (groups == null) ? validator.validate(obj) : validator.validate(obj, groups);
                if (!objViolations.isEmpty()) {
                    violations.addAll(objViolations);
                    objectsThatFailedValidation.add(obj);
                }
            }
        }

        // If it came back clean we're done - just return.
        if (violations.isEmpty()) {
            return;
        }

        // If we reach here then it didn't come back clean. We have at least one validation error.
        throw new ClientDataValidationError(objectsThatFailedValidation, violations, groups);
    }

}
