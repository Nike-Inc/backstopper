package com.nike.backstopper.annotation.post.processor.model;

import org.junit.Test;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link ElementDetails}.
 *
 * @author Andrey Tsarenko
 */
public class ElementDetailsTest {

    @Test
    public void getters() {
        Set<AnnotationDetails> annotationDetails = Collections.singleton(
                new AnnotationDetails("annotationName", mock(AnnotationMirror.class), Collections.emptyMap()));
        ElementDetails elementDetails = new ElementDetails(annotationDetails);

        assertThat(elementDetails.getAnnotationDetails()).isEqualTo(annotationDetails);
    }

    @Test
    public void equals() {
        ElementDetails firstElementDetails = new ElementDetails(Collections.singleton(
                new AnnotationDetails("annotationName", mock(AnnotationMirror.class), Collections.emptyMap())));
        ElementDetails secondElementDetails = new ElementDetails(Collections.singleton(
                new AnnotationDetails("annotationName", mock(AnnotationMirror.class), Collections.emptyMap())));
        ElementDetails thirdElementDetails = new ElementDetails(Collections.singleton(
                new AnnotationDetails("secondAnnotationName", mock(AnnotationMirror.class), Collections.emptyMap())));

        assertThat(firstElementDetails).isEqualTo(firstElementDetails);
        assertThat(firstElementDetails).isEqualTo(secondElementDetails);

        assertThat(firstElementDetails).isNotEqualTo(thirdElementDetails);
        assertThat(firstElementDetails).isNotEqualTo(new Object());
        assertThat(firstElementDetails).isNotEqualTo(null);
    }

    @Test
    public void hashcode() {
        Set<AnnotationDetails> annotationDetails = Collections.singleton(
                new AnnotationDetails("annotationName", mock(AnnotationMirror.class), Collections.emptyMap()));
        ElementDetails elementDetails = new ElementDetails(annotationDetails);

        assertThat(elementDetails.hashCode()).isEqualTo(
                Objects.hash(annotationDetails));
    }

}
