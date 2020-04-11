package com.nike.backstopper.annotation.post.processor.model;

import org.junit.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link AnnotationValueDetails}.
 *
 * @author Andrey Tsarenko
 */
public class AnnotationValueDetailsTest {

    @Test
    public void getters() {
        AnnotationValueDetails annotationValueDetails = new AnnotationValueDetails("value", "returnType");

        assertThat(annotationValueDetails.getValue()).isEqualTo("value");
        assertThat(annotationValueDetails.getReturnType()).isEqualTo("returnType");
    }

    @Test
    public void equals() {
        AnnotationValueDetails firstAnnotationValueDetails = new AnnotationValueDetails("value", "returnType");
        AnnotationValueDetails secondAnnotationValueDetails = new AnnotationValueDetails("value", "returnType");
        AnnotationValueDetails thirdAnnotationValueDetails = new AnnotationValueDetails("secondValue", "returnType");

        assertThat(firstAnnotationValueDetails).isEqualTo(firstAnnotationValueDetails);
        assertThat(firstAnnotationValueDetails).isEqualTo(secondAnnotationValueDetails);

        assertThat(firstAnnotationValueDetails).isNotEqualTo(thirdAnnotationValueDetails);
        assertThat(firstAnnotationValueDetails).isNotEqualTo(new Object());
        assertThat(firstAnnotationValueDetails).isNotEqualTo(null);
    }

    @Test
    public void hashcode() {
        AnnotationValueDetails annotationValueDetails = new AnnotationValueDetails("value", "returnType");

        assertThat(annotationValueDetails.hashCode()).isEqualTo(
                Objects.hash("value", "returnType"));
    }

}
