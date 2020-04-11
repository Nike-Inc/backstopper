package com.nike.backstopper.annotation.post.processor.evaluator;

import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.AnnotationValueDetails;
import com.nike.backstopper.apierror.ApiErrorValue;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.AnnotationMirror;
import java.util.Arrays;
import java.util.List;

import static com.nike.backstopper.annotation.post.processor.ApiErrorValueProcessor.API_ERROR_VALUE_ANNOTATION_NAME;
import static com.nike.backstopper.annotation.post.processor.resolver.ApiErrorValueMetadataResolver.MESSAGE_METHOD_NAME;

/**
 * Evaluates annotations to determine if the annotation is {@link ApiErrorValue} or JSR 303 constraint annotation.
 *
 * @author Andrey Tsarenko
 */
public class AnnotationEvaluator {

    private static final String STRING_RETURN_TYPE = "java.lang.String";
    private static final String CUSTOM_CONSTRAINT_ANNOTATION_NAME = "javax.validation.Constraint";
    private static final List<String> DEFAULT_CONSTRAINT_ANNOTATION_NAMES = Arrays.asList(
            "javax.validation.constraints.AssertFalse",
            "javax.validation.constraints.AssertTrue",
            "javax.validation.constraints.DecimalMax",
            "javax.validation.constraints.DecimalMin",
            "javax.validation.constraints.Digits",
            "javax.validation.constraints.Email",
            "javax.validation.constraints.Future",
            "javax.validation.constraints.FutureOrPresent",
            "javax.validation.constraints.Max",
            "javax.validation.constraints.Min",
            "javax.validation.constraints.Negative",
            "javax.validation.constraints.NegativeOrZero",
            "javax.validation.constraints.NotBlank",
            "javax.validation.constraints.NotEmpty",
            "javax.validation.constraints.NotNull",
            "javax.validation.constraints.Null",
            "javax.validation.constraints.Past",
            "javax.validation.constraints.PastOrPresent",
            "javax.validation.constraints.Pattern",
            "javax.validation.constraints.Positive",
            "javax.validation.constraints.PositiveOrZero",
            "javax.validation.constraints.Size");

    private final ElementExtractor elementExtractor;

    public AnnotationEvaluator(ElementExtractor elementExtractor) {
        this.elementExtractor = elementExtractor;
    }

    /**
     * Determines if a annotation is {@link ApiErrorValue}.
     *
     * @param annotationDetails the annotation to check.
     * @return {@code true} only if the annotation is {@link ApiErrorValue}, otherwise returns {@code false}.
     */
    public boolean isApiErrorValueAnnotation(@NotNull AnnotationDetails annotationDetails) {
        return API_ERROR_VALUE_ANNOTATION_NAME.equals(annotationDetails.getAnnotationName());
    }

    /**
     * Determines if a annotation is JSR 303 constraint.
     *
     * @param annotationDetails the annotation to check.
     * @return {@code true} if the annotation is a JSR 303 constraint.
     * or a valid constraint annotation such as Hibernate/custom, otherwise returns {@code false}.
     */
    public boolean isValidationConstraintAnnotation(@NotNull AnnotationDetails annotationDetails) {
        return DEFAULT_CONSTRAINT_ANNOTATION_NAMES.contains(annotationDetails.getAnnotationName())
                || (isCustomConstraintAnnotation(annotationDetails) && isContainsMessage(annotationDetails));
    }

    private boolean isCustomConstraintAnnotation(AnnotationDetails annotationDetails) {
        List<? extends AnnotationMirror> annotationMirrors = annotationDetails.getAnnotationMirror()
                .getAnnotationType()
                .asElement()
                .getAnnotationMirrors();
        boolean customConstraintAnnotation = false;

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            String annotationName = elementExtractor.getAnnotationName(annotationMirror);

            if (CUSTOM_CONSTRAINT_ANNOTATION_NAME.equals(annotationName)) {
                customConstraintAnnotation = true;
                break;
            }
        }
        return customConstraintAnnotation;
    }

    private boolean isContainsMessage(AnnotationDetails annotationDetails) {
        AnnotationValueDetails messageDetails = annotationDetails.getAnnotationValueDetails(MESSAGE_METHOD_NAME);
        return messageDetails != null && STRING_RETURN_TYPE.equals(messageDetails.getReturnType());
    }

}
