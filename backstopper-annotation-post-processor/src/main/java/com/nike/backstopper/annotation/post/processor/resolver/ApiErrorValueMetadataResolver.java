package com.nike.backstopper.annotation.post.processor.resolver;

import com.nike.backstopper.annotation.post.processor.evaluator.AnnotationEvaluator;
import com.nike.backstopper.annotation.post.processor.exception.ApiErrorValueMetadataResolverException;
import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.ElementDetails;
import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.model.ApiErrorValueMetadata;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.nike.backstopper.annotation.post.processor.ApiErrorValueProcessor.API_ERROR_VALUE_ANNOTATION_NAME;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Resolves element's details to obtain {@link ApiErrorValueMetadata}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataResolver {

    /**
     * Method name {@code message} in constraint annotations.
     */
    public static final String MESSAGE_METHOD_NAME = "message";
    private static final String ERROR_CODE_METHOD_NAME = "errorCode";
    private static final String HTTP_STATUS_CODE_METHOD_NAME = "httpStatusCode";

    private final Messager messager;
    private final ElementExtractor elementExtractor;
    private final AnnotationEvaluator annotationEvaluator;

    public ApiErrorValueMetadataResolver(Messager messager, ElementExtractor elementExtractor,
                                         AnnotationEvaluator annotationEvaluator) {
        this.messager = messager;
        this.elementExtractor = elementExtractor;
        this.annotationEvaluator = annotationEvaluator;
    }

    /**
     * Resolves {@link ApiErrorValueMetadata} for each element.
     * <p>
     * The element that is annotated by {@link ApiErrorValue} will be processed only if:
     * 1. {@link ApiErrorValue#errorCode()} value is not {@code null} or blank
     * 2. {@link ApiErrorValue#httpStatusCode()} value is not negative
     * 3. the element annotated/inherited JSR 303 constraint annotation
     * or a valid constraint annotation such as Hibernate/custom
     * 4. the constraint annotation has no {@code null} or blank {@link String} value for a {@code message} method
     * <p>
     * One {@link ApiErrorValueMetadata} contains one {@link ApiErrorValue} and N constraint annotations,
     * if the element contains several {@link ApiErrorValue}, only the first will be resolved.
     * <p>
     * Collision resolution:
     * Hibernate's constraint annotations annotated with JSR 303 constraint annotations:
     * {@code NotBlank} annotated by {@code @NotNull} - constraint annotation over constraint annotation,
     * in this case {@code @NotNull} always has {@code message} method with a default value,
     * and this message is not used when an exception occurs.
     * But nevertheless, such annotations will also be resolved with their default values,
     * although they will not be used, this allows to map messages with {@code ValidationMessages.properties}
     * by {@code ApiErrorValidationMessagesMapper} in runtime.
     *
     * @param elementDetails the elements to resolve.
     * @return immutable {@link ApiErrorValue} metadata.
     * @throws ApiErrorValueMetadataResolverException if the resolving failed.
     */
    public Set<ApiErrorValueMetadata> resolve(@NotNull Set<ElementDetails> elementDetails) {
        try {

            Set<ApiErrorValueMetadata> apiErrorValuesMetadata = new HashSet<>();
            for (ElementDetails elementDetail : elementDetails) {
                apiErrorValuesMetadata.addAll(resolveApiErrorValuesMetadata(elementDetail));
            }
            return Collections.unmodifiableSet(apiErrorValuesMetadata);
        } catch (Exception e) {
            throw new ApiErrorValueMetadataResolverException("Unable to resolve elementDetails: " + e.getMessage(), e);
        }
    }

    private Set<ApiErrorValueMetadata> resolveApiErrorValuesMetadata(ElementDetails elementDetails) {
        String errorCode = null;
        Integer httpStatusCode = null;
        Set<String> messages = new HashSet<>();
        boolean apiErrorValueAnnotationResolved = false;

        for (AnnotationDetails annotationDetails : elementDetails.getAnnotationDetails()) {

            if (!apiErrorValueAnnotationResolved && annotationEvaluator.isApiErrorValueAnnotation(annotationDetails)) {
                String errorCodeValue = elementExtractor.getAnnotationStringValue(
                        annotationDetails, ERROR_CODE_METHOD_NAME);
                Integer httpStatusCodeValue = elementExtractor.getAnnotationIntValue(
                        annotationDetails, HTTP_STATUS_CODE_METHOD_NAME);

                if (errorCodeValue != null && httpStatusCodeValue != null && httpStatusCodeValue >= 0) {
                    errorCode = errorCodeValue;
                    httpStatusCode = httpStatusCodeValue;
                    apiErrorValueAnnotationResolved = true;
                } else {
                    messager.printMessage(ERROR, String.format("%s annotation processing failed, invalid 'values': %s",
                            API_ERROR_VALUE_ANNOTATION_NAME, annotationDetails.getAnnotationMirror()));
                }

            } else if (apiErrorValueAnnotationResolved && annotationEvaluator.isApiErrorValueAnnotation(annotationDetails)) {
                // it could be a valid case with overriding an inherited ApiErrorValue annotation.
                messager.printMessage(NOTE, String.format("%s annotation skipped as duplicate: %s",
                        API_ERROR_VALUE_ANNOTATION_NAME, annotationDetails.getAnnotationMirror()));

            } else if (annotationEvaluator.isValidationConstraintAnnotation(annotationDetails)) {
                String messageValue = elementExtractor.getAnnotationStringValue(annotationDetails, MESSAGE_METHOD_NAME);

                if (messageValue != null) {
                    messages.add(messageValue);
                } else {
                    messager.printMessage(ERROR, String.format("%s annotation processing failed, invalid message's 'value': %s",
                            annotationDetails.getAnnotationName(), annotationDetails.getAnnotationMirror()));
                }
            }
        }
        return buildApiErrorValuesMetadata(errorCode, httpStatusCode, messages);
    }

    private Set<ApiErrorValueMetadata> buildApiErrorValuesMetadata(String errorCode, Integer httpStatusCode,
                                                                   Set<String> messages) {
        if (errorCode != null && httpStatusCode != null && !messages.isEmpty()) {
            Set<ApiErrorValueMetadata> apiErrorValuesMetadata = new HashSet<>();

            for (String message : messages) {
                apiErrorValuesMetadata.add(new ApiErrorValueMetadata(errorCode, httpStatusCode, message));
            }
            return Collections.unmodifiableSet(apiErrorValuesMetadata);

        }
        messager.printMessage(ERROR, String.format("%s annotation processing failed, invalid values - "
                        + "'errorCode': %s, 'httpStatusCode': %s, constraint annotation's 'message': %s",
                API_ERROR_VALUE_ANNOTATION_NAME, errorCode, httpStatusCode, messages));
        return Collections.emptySet();
    }

}
