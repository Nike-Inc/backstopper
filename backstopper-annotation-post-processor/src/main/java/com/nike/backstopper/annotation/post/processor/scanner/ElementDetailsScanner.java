package com.nike.backstopper.annotation.post.processor.scanner;

import com.nike.backstopper.annotation.post.processor.exception.ElementDetailsScannerException;
import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.AnnotationValueDetails;
import com.nike.backstopper.annotation.post.processor.model.ElementDetails;
import com.nike.backstopper.apierror.ApiErrorValue;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Scans annotated elements to obtains {@link AnnotationDetails}.
 *
 * @author Andrey Tsarenko
 */
public class ElementDetailsScanner {

    private final ElementExtractor elementExtractor;

    public ElementDetailsScanner(ElementExtractor elementExtractor) {
        this.elementExtractor = elementExtractor;
    }

    /**
     * Scans {@link Element}s annotated with {@link ApiErrorValue} and obtains all annotation details:
     * 1. annotations from the target element
     * 2. annotations from the target annotations including inherited annotations
     * Provides the ability to find inherited/custom JSR 303 constraint annotations.
     *
     * @param elements the elements annotated with {@link ApiErrorValue}.
     * @return immutable element details.
     * @throws ElementDetailsScannerException if the scanning failed.
     */
    public Set<ElementDetails> scan(@NotNull Set<? extends Element> elements) {
        try {

            Set<ElementDetails> elementDetails = new HashSet<>();
            for (Element element : elements) {
                Set<AnnotationDetails> annotationDetails = scanElement(element);
                elementDetails.add(new ElementDetails(annotationDetails));
            }
            return Collections.unmodifiableSet(elementDetails);
        } catch (Exception e) {
            throw new ElementDetailsScannerException("Unable to scan elements: " + e.getMessage(), e);
        }
    }

    private Set<AnnotationDetails> scanElement(Element element) {
        Queue<Element> elementQueue = new LinkedList<>(Collections.singleton(element));
        Set<AnnotationDetails> annotationDetailsSet = new LinkedHashSet<>();
        // scan by queue & while to avoid recursion.
        while (!elementQueue.isEmpty()) {
            for (AnnotationMirror annotationMirror : elementQueue.poll().getAnnotationMirrors()) {

                AnnotationDetails annotationDetails = scanAnnotationMirror(annotationMirror);
                // check by annotationName & annotationMethodByValue's map to avoid endless loop.
                if (annotationDetailsSet.add(annotationDetails)) {
                    elementQueue.add(annotationMirror.getAnnotationType().asElement());
                }
            }
        }

        return Collections.unmodifiableSet(annotationDetailsSet);
    }

    private AnnotationDetails scanAnnotationMirror(AnnotationMirror annotationMirror) {
        Map<String, AnnotationValueDetails> annotationMethodByValue = new HashMap<>();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationElement
                : elementExtractor.getExecutableAnnotationValues(annotationMirror).entrySet()) {

            String executionName = elementExtractor.getExecutionName(annotationElement.getKey());
            AnnotationValueDetails annotationValueDetails = new AnnotationValueDetails(
                    annotationElement.getValue().getValue(),
                    elementExtractor.getExecutionReturnType(annotationElement.getKey()));

            annotationMethodByValue.put(executionName, annotationValueDetails);
        }

        return new AnnotationDetails(elementExtractor.getAnnotationName(annotationMirror),
                annotationMirror, Collections.unmodifiableMap(annotationMethodByValue));
    }

}
