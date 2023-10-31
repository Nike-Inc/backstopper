package com.nike.backstopper.annotation.post.processor.model;

import com.nike.backstopper.annotation.post.processor.scanner.ElementDetailsScanner;

import javax.lang.model.element.AnnotationMirror;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The immutable model is used to represent {@link AnnotationMirror} details.
 * {@link #equals(Object)} and {@link #hashCode()} use only {@link #annotationName} and {@link #annotationMethodByValue}
 * to compare this object and store in {@link Set}.
 *
 * @author Andrey Tsarenko
 * @see ElementDetailsScanner
 */
public class AnnotationDetails {

    private final String annotationName;
    private final AnnotationMirror annotationMirror;
    private final Map<String, AnnotationValueDetails> annotationMethodByValue;

    public AnnotationDetails(String annotationName, AnnotationMirror annotationMirror,
                             Map<String, AnnotationValueDetails> annotationMethodByValue) {
        this.annotationName = annotationName;
        this.annotationMirror = annotationMirror;
        this.annotationMethodByValue = annotationMethodByValue;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public AnnotationMirror getAnnotationMirror() {
        return annotationMirror;
    }

    public AnnotationValueDetails getAnnotationValueDetails(String annotationMethodName) {
        return annotationMethodByValue.get(annotationMethodName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationDetails that = (AnnotationDetails) o;
        return Objects.equals(annotationName, that.annotationName)
                && Objects.equals(annotationMethodByValue, that.annotationMethodByValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationName, annotationMethodByValue);
    }

}
