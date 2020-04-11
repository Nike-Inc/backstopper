package com.nike.backstopper.annotation.post.processor.resolver;

import com.nike.backstopper.annotation.post.processor.evaluator.AnnotationEvaluator;
import com.nike.backstopper.annotation.post.processor.exception.ApiErrorValueMetadataResolverException;
import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.nike.backstopper.annotation.post.processor.model.AnnotationValueDetails;
import com.nike.backstopper.annotation.post.processor.model.ElementDetails;
import com.nike.backstopper.model.ApiErrorValueMetadata;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.processing.Messager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.nike.backstopper.annotation.post.processor.ApiErrorValueProcessor.API_ERROR_VALUE_ANNOTATION_NAME;
import static com.nike.backstopper.annotation.post.processor.TestUtils.buildAnnotationDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link ApiErrorValueMetadataResolver}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataResolverTest {

    private ApiErrorValueMetadataResolver apiErrorValueMetadataResolver;
    private AnnotationDetails apiErrorValueAnnotationDetails;

    @Before
    public void setUp() {
        ElementExtractor elementExtractor = new ElementExtractor(null);
        Messager messager = mock(Messager.class, RETURNS_DEEP_STUBS);
        apiErrorValueMetadataResolver = new ApiErrorValueMetadataResolver(
                messager, elementExtractor, new AnnotationEvaluator(elementExtractor));

        Map<String, AnnotationValueDetails> apiErrorValueAnnotationValues = new HashMap<>();
        apiErrorValueAnnotationValues.put("errorCode",
                new AnnotationValueDetails("errorCodeValue", "java.lang.String"));
        apiErrorValueAnnotationValues.put("httpStatusCode",
                new AnnotationValueDetails(400, "java.lang.Integer"));

        apiErrorValueAnnotationDetails = buildAnnotationDetails(
                API_ERROR_VALUE_ANNOTATION_NAME, apiErrorValueAnnotationValues);
    }

    @Test
    public void resolve() {
        AnnotationDetails assertFalseAnnotationDetails = buildAnnotationDetails(
                "javax.validation.Constraint", "javax.validation.constraints.AssertFalse",
                "message", "assertFalseMessageValue", "java.lang.String");
        AnnotationDetails customValidationAnnotationDetails = buildAnnotationDetails(
                "javax.validation.Constraint", "com.test.constraints.CustomValidation",
                "message", "customValidationMessageValue", "java.lang.String");

        Set<AnnotationDetails> annotationDetails = new HashSet<>();
        annotationDetails.add(apiErrorValueAnnotationDetails);
        annotationDetails.add(assertFalseAnnotationDetails);
        annotationDetails.add(customValidationAnnotationDetails);
        Set<ElementDetails> elementDetails = Collections.singleton(new ElementDetails(annotationDetails));

        assertThat(apiErrorValueMetadataResolver.resolve(elementDetails)).containsOnly(
                new ApiErrorValueMetadata("errorCodeValue", 400, "assertFalseMessageValue"),
                new ApiErrorValueMetadata("errorCodeValue", 400, "customValidationMessageValue"));
    }

    @Test
    public void resolveByUnacceptableCustomValidationAnnotation() {
        AnnotationDetails firstUnacceptableAnnotationDetails = buildAnnotationDetails(
                null, "com.test.constraints.FirstUnacceptableCustomValidation",
                "message", "assertFalseMessageValue", "java.lang.String");
        AnnotationDetails secondUnacceptableAnnotationDetails = buildAnnotationDetails(
                null, "com.test.constraints.SecondUnacceptableCustomValidation",
                "message", "customValidationMessageValue", "java.lang.Integer");

        Set<AnnotationDetails> annotationDetails = new HashSet<>();
        annotationDetails.add(apiErrorValueAnnotationDetails);
        annotationDetails.add(firstUnacceptableAnnotationDetails);
        annotationDetails.add(secondUnacceptableAnnotationDetails);
        Set<ElementDetails> elementDetails = Collections.singleton(new ElementDetails(annotationDetails));

        assertThat(apiErrorValueMetadataResolver.resolve(elementDetails)).isEmpty();
    }

    @Test
    public void resolveWithoutValidationAnnotation() {
        Set<ElementDetails> elementDetails = Collections.singleton(
                new ElementDetails(Collections.singleton(apiErrorValueAnnotationDetails)));

        assertThat(apiErrorValueMetadataResolver.resolve(elementDetails)).isEmpty();
    }

    @Test
    public void resolveEmptyElementDetails() {
        assertThat(apiErrorValueMetadataResolver.resolve(Collections.emptySet())).isEmpty();
    }

    @Test(expected = ApiErrorValueMetadataResolverException.class)
    public void resolveFailed() {
        apiErrorValueMetadataResolver.resolve(null);
    }

}
