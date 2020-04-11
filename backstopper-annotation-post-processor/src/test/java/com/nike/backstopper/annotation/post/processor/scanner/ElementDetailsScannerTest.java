package com.nike.backstopper.annotation.post.processor.scanner;

import com.nike.backstopper.annotation.post.processor.exception.ElementDetailsScannerException;
import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.AnnotationValueDetails;
import com.nike.backstopper.annotation.post.processor.model.ElementDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ElementDetailsScanner}.
 *
 * @author Andrey Tsarenko
 */
@RunWith(MockitoJUnitRunner.class)
public class ElementDetailsScannerTest {

    @Mock
    private ElementExtractor elementExtractor;

    @InjectMocks
    private ElementDetailsScanner elementDetailsScanner;

    @Test
    public void scan() {
        Element element = mock(Element.class);
        Element inheritAnnotationElement = mock(Element.class);
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class, RETURNS_DEEP_STUBS);
        ExecutableElement executableElement = mock(ExecutableElement.class);
        AnnotationValue annotationValue = mock(AnnotationValue.class);

        when(element.getAnnotationMirrors())
                .thenAnswer(invocation -> Collections.singletonList(annotationMirror));
        when(inheritAnnotationElement.getAnnotationMirrors())
                .thenReturn(Collections.emptyList());
        when(annotationMirror.getAnnotationType().asElement())
                .thenReturn(inheritAnnotationElement);
        when(annotationValue.getValue())
                .thenReturn("messageValue");
        when(elementExtractor.getAnnotationName(annotationMirror))
                .thenReturn("javax.validation.constraints.AssertFalse");
        when(elementExtractor.getExecutableAnnotationValues(annotationMirror))
                .thenAnswer(invocation -> Collections.singletonMap(executableElement, annotationValue));
        when(elementExtractor.getExecutionName(executableElement))
                .thenReturn("message");
        when(elementExtractor.getExecutionReturnType(executableElement))
                .thenReturn("java.lang.String");

        AnnotationValueDetails expectedAnnotationValueDetails = new AnnotationValueDetails(
                "messageValue", "java.lang.String");
        AnnotationDetails expectedAnnotationDetails = new AnnotationDetails(
                "javax.validation.constraints.AssertFalse", annotationMirror,
                Collections.singletonMap("message", expectedAnnotationValueDetails));
        ElementDetails expectedElementDetails = new ElementDetails(Collections.singleton(expectedAnnotationDetails));

        assertThat(elementDetailsScanner.scan(Collections.singleton(element))).containsOnly(expectedElementDetails);
    }

    @Test
    public void scanEmptyElements() {
        assertThat(elementDetailsScanner.scan(Collections.emptySet())).isEmpty();
    }

    @Test(expected = ElementDetailsScannerException.class)
    public void scanFailed() {
        elementDetailsScanner.scan(null);
    }

}
