package com.nike.backstopper.annotation.post.processor.extractor;

import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.Map;

/**
 * Extracts values from program elements:
 * {@link AnnotationMirror}
 * {@link ExecutableElement}
 * {@link AnnotationDetails}
 *
 * @author Andrey Tsarenko
 */
public class ElementExtractor {

    private final Elements elementUtils;

    public ElementExtractor(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }

    /**
     * Returns values from {@link AnnotationMirror} elements, including methods by values.
     *
     * @param annotationMirror the annotation to extract.
     * @return immutable map of methods by values.
     */
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getExecutableAnnotationValues(
            @NotNull AnnotationMirror annotationMirror) {

        return Collections.unmodifiableMap(elementUtils.getElementValuesWithDefaults(annotationMirror));
    }

    /**
     * Returns a name from {@link ExecutableElement}.
     *
     * @param executableElement a execution method.
     * @return the name from the execution method.
     */
    public String getExecutionName(@NotNull ExecutableElement executableElement) {
        return executableElement.getSimpleName().toString();
    }

    /**
     * Returns a return type from {@link ExecutableElement}.
     *
     * @param executableElement a execution method.
     * @return the return type from the execution method.
     */
    public String getExecutionReturnType(@NotNull ExecutableElement executableElement) {
        return executableElement.getReturnType().toString();
    }

    /**
     * Returns a name from {@link AnnotationMirror}.
     *
     * @param annotationMirror a annotation.
     * @return the name from the annotation.
     */
    public String getAnnotationName(@NotNull AnnotationMirror annotationMirror) {
        return annotationMirror.getAnnotationType().toString();
    }

    /**
     * Returns {@link String} value from an annotation's method.
     *
     * @param annotationDetails the annotation details.
     * @param methodName        a method's name to extract.
     * @return the value if a value's type instanceof {@link String} and the value is not {@code null} or blank,
     * otherwise returns {@code null}.
     */
    @Nullable
    public String getAnnotationStringValue(@NotNull AnnotationDetails annotationDetails, @NotNull String methodName) {
        Object value = annotationDetails.getAnnotationValueDetails(methodName).getValue();

        if (value instanceof String) {
            String stringValue = (String) value;

            if (!stringValue.isEmpty() && !stringValue.trim().isEmpty()) {
                return stringValue;
            }
        }
        return null;
    }

    /**
     * Returns {@link Integer} value from an annotation's method.
     *
     * @param annotationDetails the annotation details.
     * @param methodName        a method's name to extract.
     * @return the value if a value's type instanceof {@link Integer} and the value is not {@code null},
     * otherwise returns {@code null}.
     */
    @Nullable
    public Integer getAnnotationIntValue(@NotNull AnnotationDetails annotationDetails, @NotNull String methodName) {
        Object value = annotationDetails.getAnnotationValueDetails(methodName).getValue();

        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

}
