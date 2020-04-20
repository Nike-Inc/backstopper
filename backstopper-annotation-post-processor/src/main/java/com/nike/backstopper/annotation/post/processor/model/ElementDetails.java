package com.nike.backstopper.annotation.post.processor.model;

import javax.lang.model.element.Element;
import java.util.Objects;
import java.util.Set;

/**
 * The immutable model is used to represent {@link Element} details.
 *
 * @author Andrey Tsarenko
 */
public class ElementDetails {

    private final Set<AnnotationDetails> annotationDetails;

    public ElementDetails(Set<AnnotationDetails> annotationDetails) {
        this.annotationDetails = annotationDetails;
    }

    public Set<AnnotationDetails> getAnnotationDetails() {
        return annotationDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElementDetails that = (ElementDetails) o;
        return Objects.equals(annotationDetails, that.annotationDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationDetails);
    }

}
