package com.nike.backstopper.annotation.post.processor.model;

import org.junit.Test;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link AnnotationDetails}.
 *
 * @author Andrey Tsarenko
 */
public class AnnotationDetailsTest {

    @Test
    public void getters() {
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class);
        AnnotationValueDetails annotationValueDetails = new AnnotationValueDetails("value", "returnType");
        AnnotationDetails annotationDetails = new AnnotationDetails("annotationName", annotationMirror,
                Collections.singletonMap("annotationMethod", annotationValueDetails));

        assertThat(annotationDetails.getAnnotationName()).isEqualTo("annotationName");
        assertThat(annotationDetails.getAnnotationMirror()).isEqualTo(annotationMirror);
        assertThat(annotationDetails.getAnnotationValueDetails("annotationMethod")).isEqualTo(annotationValueDetails);
    }

    @Test
    public void equals() {
        AnnotationDetails firstAnnotationDetails = new AnnotationDetails(
                "annotationName", mock(AnnotationMirror.class), Collections.emptyMap());
        AnnotationDetails secondAnnotationDetails = new AnnotationDetails(
                "annotationName", mock(AnnotationMirror.class), Collections.emptyMap());
        AnnotationDetails thirdAnnotationDetails = new AnnotationDetails(
                "secondAnnotationName", mock(AnnotationMirror.class), Collections.emptyMap());

        assertThat(firstAnnotationDetails).isEqualTo(firstAnnotationDetails);
        assertThat(firstAnnotationDetails).isEqualTo(secondAnnotationDetails);

        assertThat(firstAnnotationDetails).isNotEqualTo(thirdAnnotationDetails);
        assertThat(firstAnnotationDetails).isNotEqualTo(new Object());
        assertThat(firstAnnotationDetails).isNotEqualTo(null);
    }

    @Test
    public void hashcode() {
        AnnotationDetails annotationDetails = new AnnotationDetails(
                "annotationName", mock(AnnotationMirror.class), Collections.emptyMap());

        assertThat(annotationDetails.hashCode()).isEqualTo(
                Objects.hash("annotationName", Collections.emptyMap()));
    }

}
