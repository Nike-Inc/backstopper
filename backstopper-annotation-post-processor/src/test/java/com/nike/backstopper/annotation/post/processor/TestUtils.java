package com.nike.backstopper.annotation.post.processor;

import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.AnnotationValueDetails;
import com.nike.backstopper.model.ApiErrorValueMetadata;

import javax.lang.model.element.AnnotationMirror;
import javax.tools.JavaFileObject;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test utilities for:
 * 1. building test data for tests
 * 2. reading {@link ApiErrorValueMetadata}
 *
 * @author Andrey Tsarenko
 */
public final class TestUtils {

    private TestUtils() {
        // do nothing.
    }

    public static AnnotationDetails buildAnnotationDetailsByName(String annotationName) {
        return new AnnotationDetails(annotationName, null, null);
    }

    public static AnnotationDetails buildAnnotationDetailsByValue(Object annotationValue) {
        AnnotationDetails annotationDetails = mock(AnnotationDetails.class);
        AnnotationValueDetails annotationValueDetails = mock(AnnotationValueDetails.class);

        when(annotationValueDetails.getValue())
                .thenReturn(annotationValue);
        when(annotationDetails.getAnnotationValueDetails("annotationMethodName"))
                .thenReturn(annotationValueDetails);

        return annotationDetails;
    }

    public static AnnotationDetails buildAnnotationDetails(String annotationName,
                                                           Map<String, AnnotationValueDetails> annotationMethodByValue) {
        return new AnnotationDetails(annotationName, null, annotationMethodByValue);
    }

    public static AnnotationDetails buildAnnotationDetails(String inheritAnnotationName,
                                                           String annotationMethodName,
                                                           String annotationValue,
                                                           String annotationValueReturnType) {
        return buildAnnotationDetails(inheritAnnotationName, null,
                annotationMethodName, annotationValue, annotationValueReturnType);
    }

    public static AnnotationDetails buildAnnotationDetails(String inheritAnnotationName,
                                                           String annotationMame,
                                                           String annotationMethodMame,
                                                           String annotationValue,
                                                           String annotationValueReturnType) {
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class, RETURNS_DEEP_STUBS);
        AnnotationDetails annotationDetails = mock(AnnotationDetails.class, RETURNS_DEEP_STUBS);

        when(annotationMirror.getAnnotationType().toString())
                .thenReturn(inheritAnnotationName);
        when(annotationDetails.getAnnotationName())
                .thenReturn(annotationMame);
        when(annotationDetails.getAnnotationValueDetails(annotationMethodMame))
                .thenReturn(new AnnotationValueDetails(annotationValue, annotationValueReturnType));
        when(annotationDetails.getAnnotationMirror()
                .getAnnotationType()
                .asElement()
                .getAnnotationMirrors())
                .thenAnswer(invocation -> Collections.singletonList(annotationMirror));

        return annotationDetails;
    }

    public static Set<ApiErrorValueMetadata> readApiErrorValuesMetadata(JavaFileObject javaFileObject) {
        try {
            return readApiErrorValuesMetadata(javaFileObject.openInputStream());

        } catch (Exception e) {
            throw new TestUtilsException("Unable to read ApiErrorValueMetadata from javaFileObject: " + javaFileObject, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<ApiErrorValueMetadata> readApiErrorValuesMetadata(InputStream inputStream) {
        try {

            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (Set<ApiErrorValueMetadata>) objectInputStream.readObject();
        } catch (Exception e) {
            throw new TestUtilsException("Unable to read ApiErrorValueMetadata from inputStream", e);
        }
    }

    private static class TestUtilsException extends RuntimeException {

        public TestUtilsException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
