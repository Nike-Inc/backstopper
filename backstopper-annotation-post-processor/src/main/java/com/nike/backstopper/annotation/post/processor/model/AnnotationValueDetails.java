package com.nike.backstopper.annotation.post.processor.model;

import java.util.Objects;

/**
 * The immutable model is used to represent an annotation's value details.
 *
 * @author Andrey Tsarenko
 */
public class AnnotationValueDetails {

    private final Object value;
    private final String returnType;

    public AnnotationValueDetails(Object value, String returnType) {
        this.value = value;
        this.returnType = returnType;
    }

    public Object getValue() {
        return value;
    }

    public String getReturnType() {
        return returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationValueDetails that = (AnnotationValueDetails) o;
        return Objects.equals(value, that.value)
                && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, returnType);
    }

}
