package com.nike.backstopper.exception;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link ServersideValidationError}. Since there isn't really much functionality,
 * this just verifies that the constructors/etc work without blowing up.
 *
 * @author Nic Munroe
 */
public class ServersideValidationErrorTest {

    @Test
    public void verifyObjectThatFailedValidationIsSet() {
        Object someObj = new Object();
        ServersideValidationError ex = new ServersideValidationError(someObj, null);
        assertThat(ex.getObjectThatFailedValidation(), is(someObj));
    }

    @Test
    public void verifyViolationsIsSet() {
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ServersideValidationError ex = new ServersideValidationError(null, violations);
        assertThat(ex.getViolations(), is(violations));
    }

}
