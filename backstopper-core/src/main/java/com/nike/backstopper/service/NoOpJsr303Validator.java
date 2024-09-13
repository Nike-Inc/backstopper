package com.nike.backstopper.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

import static java.util.Collections.emptySet;

/**
 * Simple no-op implementation of the JSR 303 Bean Validation {@link Validator} - useful for satisfying dependency
 * injection requirements in cases where you won't actually use the JSR 303 {@link Validator} for any real work
 * and don't want your project to pull in a dependency on a real JSR 303 implementation.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class NoOpJsr303Validator implements Validator, ExecutableValidator {

    public static final NoOpJsr303Validator SINGLETON_IMPL = new NoOpJsr303Validator();

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
                                                         Class<?>... groups) {
        return emptySet();
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        throw new ValidationException(this.getClass().getName() + " does not implement getConstraintsForClass()");
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new ValidationException(this.getClass().getName() + " does not implement unwrap()");
    }

    @Override
    public ExecutableValidator forExecutables() {
        return this;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateParameters(
        T object, Method method, Object[] parameterValues, Class<?>... groups
    ) {
        return emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateReturnValue(
        T object, Method method, Object returnValue, Class<?>... groups
    ) {
        return emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(
        Constructor<? extends T> constructor, Object[] parameterValues, Class<?>... groups
    ) {
        return emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(
        Constructor<? extends T> constructor, T createdObject, Class<?>... groups
    ) {
        return emptySet();
    }
}
