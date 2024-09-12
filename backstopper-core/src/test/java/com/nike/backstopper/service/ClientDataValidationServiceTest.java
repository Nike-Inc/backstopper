package com.nike.backstopper.service;

import com.nike.backstopper.exception.ClientDataValidationError;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Verifies the functionality of {@link com.nike.backstopper.service.ClientDataValidationService}
 */
public class ClientDataValidationServiceTest {

    private ClientDataValidationService validationServiceSpy;
    private Validator validatorMock;

    @Before
    public void setupMethod() {
        validatorMock = mock(Validator.class);
        validationServiceSpy = spy(new ClientDataValidationService(validatorMock));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldExplodeIfPassedInNullValidatorForConstructor() {
        new ClientDataValidationService(null);
    }

    @Test
    public void shouldDelegateValidateObjectsFailFastMethod() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        validationServiceSpy.validateObjectsFailFast(obj1, obj2);
        verify(validationServiceSpy).validateObjectsWithGroupsFailFast((Class<?>[])null, obj1, obj2);
    }

    @Test
    public void shouldDelegateValidateObjectsWithGroupFailFastMethod() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        validationServiceSpy.validateObjectsWithGroupFailFast(Default.class, obj1, obj2);
        verify(validationServiceSpy).validateObjectsWithGroupsFailFast(new Class<?>[]{Default.class}, obj1, obj2);
    }

    @Test
    public void shouldDelegateValidateObjectsWithGroupsFailFastCollectionMethod() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast(Arrays.asList(Default.class, String.class), obj1, obj2);
        verify(validationServiceSpy).validateObjectsWithGroupsFailFast(new Class<?>[]{Default.class, String.class}, obj1, obj2);
    }

    @Test
    public void shouldDelegateValidateObjectsWithGroupsFailFastCollectionMethodWithNullGroups() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast((Collection<Class<?>>)null, obj1, obj2);
        verify(validationServiceSpy).validateObjectsWithGroupsFailFast((Class<?>[])null, obj1, obj2);
    }

    @Test
    public void shouldDelegateValidateObjectsWithGroupsFailFastCollectionMethodWithEmptyGroups() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast(Collections.<Class<?>>emptyList(), obj1, obj2);
        verify(validationServiceSpy).validateObjectsWithGroupsFailFast((Class<?>[])null, obj1, obj2);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldDoNothingIfObjectsArrayIsNull() {
        validationServiceSpy.validateObjectsWithGroupsFailFast((Class<?>[])null, (Object[])null);
        verifyNoMoreInteractions(validatorMock);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldDoNothingIfObjectsArrayIsEmpty() {
        validationServiceSpy.validateObjectsWithGroupsFailFast((Class<?>[])null, new Object[0]);
        verifyNoMoreInteractions(validatorMock);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldValidatePassedInObjectsNoGroups() {
        given(validatorMock.validate(any(), any(Class[].class))).willReturn(Collections.<ConstraintViolation<Object>>emptySet());
        Object objToValidate1 = new Object();
        Object objToValidate2 = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast((Class<?>[])null, objToValidate1, objToValidate2);
        verify(validatorMock).validate(objToValidate1);
        verify(validatorMock).validate(objToValidate2);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldValidatePassedInObjectsWithGroups() {
        given(validatorMock.validate(any(), any(Class[].class))).willReturn(Collections.<ConstraintViolation<Object>>emptySet());
        Object objToValidate1 = new Object();
        Object objToValidate2 = new Object();
        Class<?>[] groups = new Class<?>[]{Default.class, String.class};
        validationServiceSpy.validateObjectsWithGroupsFailFast(groups, objToValidate1, objToValidate2);
        verify(validatorMock).validate(objToValidate1, groups);
        verify(validatorMock).validate(objToValidate2, groups);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldNotThrowExceptionIfThereAreNoViolations() {
        given(validatorMock.validate(any(), any(Class[].class))).willReturn(Collections.<ConstraintViolation<Object>>emptySet());
        Object objToValidate = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast((Class<?>[])null, objToValidate);
        verify(validatorMock).validate(objToValidate);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldNotValidateNullObjects() {
        given(validatorMock.validate(any(), any(Class[].class))).willReturn(Collections.<ConstraintViolation<Object>>emptySet());
        Object objToValidate = new Object();
        validationServiceSpy.validateObjectsWithGroupsFailFast((Class<?>[])null, new Object[]{objToValidate, null});
        verify(validatorMock).validate(objToValidate);
        verifyNoMoreInteractions(validatorMock);
    }

    @Test
    public void validateObjectsWithGroupsFailFastShouldThrowAppropriateExceptionWhenThereAreViolations() {
        Object objToValidate1 = new Object();
        Object objToValidate2 = new Object();
        Object objToValidate3 = new Object();
        Class<?>[] groups = new Class<?>[]{Default.class, String.class};
        List<ConstraintViolation<Object>> obj1Violations = Arrays.<ConstraintViolation<Object>>asList(mock(ConstraintViolation.class));
        List<ConstraintViolation<Object>> obj3Violations = Arrays.<ConstraintViolation<Object>>asList(mock(ConstraintViolation.class), mock(ConstraintViolation.class));
        given(validatorMock.validate(objToValidate1, groups)).willReturn(new HashSet<>(obj1Violations));
        given(validatorMock.validate(objToValidate2, groups)).willReturn(Collections.<ConstraintViolation<Object>>emptySet());
        given(validatorMock.validate(objToValidate3, groups)).willReturn(new HashSet<>(obj3Violations));
        try {
            validationServiceSpy.validateObjectsWithGroupsFailFast(groups, objToValidate1, objToValidate2, objToValidate3);
            fail("Expected an exception to be thrown.");
        }
        catch(ClientDataValidationError ex) {
            verify(validatorMock).validate(objToValidate1, groups);
            verify(validatorMock).validate(objToValidate2, groups);
            verify(validatorMock).validate(objToValidate3, groups);
            assertThat(ex.getObjectsThatFailedValidation(), containsInAnyOrder(objToValidate1, objToValidate3));
            List<ConstraintViolation<Object>> expectedViolations = new ArrayList<>();
            expectedViolations.addAll(obj1Violations);
            expectedViolations.addAll(obj3Violations);
            assertThat(ex.getViolations(), containsInAnyOrder(expectedViolations.toArray()));
            assertThat(ex.getValidationGroups(), is(groups));
        }
    }
}