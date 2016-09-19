package com.nike.backstopper.service;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link NoOpJsr303Validator}.
 *
 * @author Nic Munroe
 */
public class NoOpJsr303ValidatorTest {

    private Validator noOpValidator = NoOpJsr303Validator.SINGLETON_IMPL;
    private FooClass constraintAnnotatedClass = new FooClass();

    @Test
    public void validation_methods_return_empty_sets() {
        // expect
        assertThat(noOpValidator.validate(constraintAnnotatedClass)).isEmpty();
        assertThat(noOpValidator.validateProperty(constraintAnnotatedClass, "notNullString")).isEmpty();
        assertThat(noOpValidator.validateValue(FooClass.class, "notNullString", null)).isEmpty();
    }

    @Test
    public void getConstraintsForClass_throws_ValidationException() {
        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                noOpValidator.getConstraintsForClass(FooClass.class);
            }
        });

        // then
        assertThat(ex).isInstanceOf(ValidationException.class);
    }

    @Test
    public void unwrap_throws_ValidationException() {
        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                noOpValidator.unwrap(FooClass.class);
            }
        });

        // then
        assertThat(ex).isInstanceOf(ValidationException.class);
    }

    private static class FooClass {
        @NotNull
        public String notNullString = null;
    }
}