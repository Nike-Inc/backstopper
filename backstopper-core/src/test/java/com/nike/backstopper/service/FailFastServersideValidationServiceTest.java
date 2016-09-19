package com.nike.backstopper.service;

import com.nike.backstopper.exception.ServersideValidationError;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.nike.backstopper.service.FailFastServersideValidationService}
 *
 * @author Nic Munroe
 */
public class FailFastServersideValidationServiceTest {

    @InjectMocks
    private FailFastServersideValidationService validationService;
    @Mock
    private Validator validator;

    @Before
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldNotThrowExceptionIfValidatorComesBackClean() {
        Object validateMe = new Object();
        when(validator.validate(validateMe)).thenReturn(new HashSet<ConstraintViolation<Object>>());
        validationService.validateObjectFailFast(validateMe);
    }

    @Test(expected = ServersideValidationError.class)
    public void shouldThrowExceptionIfValidatorFindsConstraintViolations() {
        Object validateMe = new Object();
        when(validator.validate(validateMe)).thenReturn(Collections.<ConstraintViolation<Object>>singleton(mock(ConstraintViolation.class)));
        validationService.validateObjectFailFast(validateMe);
    }
}
