package com.nike.backstopper.annotation.post.processor.evaluator;

import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nike.backstopper.annotation.post.processor.ApiErrorValueProcessor.API_ERROR_VALUE_ANNOTATION_NAME;
import static com.nike.backstopper.annotation.post.processor.TestUtils.buildAnnotationDetails;
import static com.nike.backstopper.annotation.post.processor.TestUtils.buildAnnotationDetailsByName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link AnnotationEvaluator}.
 *
 * @author Andrey Tsarenko
 */
@RunWith(DataProviderRunner.class)
public class AnnotationEvaluatorTest {

    private final AnnotationEvaluator annotationEvaluator = new AnnotationEvaluator(
            new ElementExtractor(mock(Elements.class)));

    @Test
    public void isApiErrorValueAnnotation() {
        AnnotationDetails annotationDetails = buildAnnotationDetailsByName(API_ERROR_VALUE_ANNOTATION_NAME);

        assertThat(annotationEvaluator.isApiErrorValueAnnotation(annotationDetails)).isTrue();
    }

    @Test
    @UseDataProvider("validationConstraintAnnotationDataProvider")
    public void isValidationConstraintAnnotation(AnnotationDetails annotationDetails) {
        assertThat(annotationEvaluator.isValidationConstraintAnnotation(annotationDetails)).isTrue();
    }

    @Test
    public void isCustomValidationConstraintAnnotation() {
        AnnotationDetails annotationDetails = buildAnnotationDetails("javax.validation.Constraint",
                "message", "messageValue", "java.lang.String");

        assertThat(annotationEvaluator.isValidationConstraintAnnotation(annotationDetails)).isTrue();
    }

    @Test
    @UseDataProvider("validationConstraintAnnotationDataProvider")
    public void isNotApiErrorValueAnnotation(AnnotationDetails annotationDetails) {
        assertThat(annotationEvaluator.isApiErrorValueAnnotation(annotationDetails)).isFalse();
    }

    @Test
    @UseDataProvider("notValidationConstraintAnnotation")
    public void isNotValidationConstraintAnnotation(AnnotationDetails annotationDetails) {
        assertThat(annotationEvaluator.isValidationConstraintAnnotation(annotationDetails)).isFalse();
    }

    @DataProvider
    public static List<List<AnnotationDetails>> validationConstraintAnnotationDataProvider() {
        return Stream.of(
                buildAnnotationDetailsByName("javax.validation.constraints.AssertFalse"),
                buildAnnotationDetailsByName("javax.validation.constraints.AssertTrue"),
                buildAnnotationDetailsByName("javax.validation.constraints.DecimalMax"),
                buildAnnotationDetailsByName("javax.validation.constraints.DecimalMin"),
                buildAnnotationDetailsByName("javax.validation.constraints.Digits"),
                buildAnnotationDetailsByName("javax.validation.constraints.Email"),
                buildAnnotationDetailsByName("javax.validation.constraints.Future"),
                buildAnnotationDetailsByName("javax.validation.constraints.FutureOrPresent"),
                buildAnnotationDetailsByName("javax.validation.constraints.Max"),
                buildAnnotationDetailsByName("javax.validation.constraints.Min"),
                buildAnnotationDetailsByName("javax.validation.constraints.Negative"),
                buildAnnotationDetailsByName("javax.validation.constraints.NegativeOrZero"),
                buildAnnotationDetailsByName("javax.validation.constraints.NotBlank"),
                buildAnnotationDetailsByName("javax.validation.constraints.NotEmpty"),
                buildAnnotationDetailsByName("javax.validation.constraints.NotNull"),
                buildAnnotationDetailsByName("javax.validation.constraints.Null"),
                buildAnnotationDetailsByName("javax.validation.constraints.Past"),
                buildAnnotationDetailsByName("javax.validation.constraints.PastOrPresent"),
                buildAnnotationDetailsByName("javax.validation.constraints.Pattern"),
                buildAnnotationDetailsByName("javax.validation.constraints.Positive"),
                buildAnnotationDetailsByName("javax.validation.constraints.PositiveOrZero"),
                buildAnnotationDetailsByName("javax.validation.constraints.Size"))
                .map(Collections::singletonList)
                .collect(Collectors.toList());
    }

    @DataProvider
    public static List<List<AnnotationDetails>> notValidationConstraintAnnotation() {
        return Stream.of(
                buildAnnotationDetails("com.test.constraints.UnacceptableCustomValidation",
                        "message", "messageValue", "java.lang.String"),
                buildAnnotationDetails("javax.validation.Constraint",
                        "custom", "customValue", "java.lang.String"),
                buildAnnotationDetails("javax.validation.Constraint",
                        "message", "messageValue", "java.lang.Integer"))
                .map(Collections::singletonList)
                .collect(Collectors.toList());
    }

}
