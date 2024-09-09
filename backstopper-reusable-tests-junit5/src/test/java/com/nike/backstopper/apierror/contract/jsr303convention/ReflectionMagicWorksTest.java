package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.internal.util.Pair;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase.extractMessageFromAnnotation;
import static com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase.generateExclusionForAnnotatedElementAndAnnotationClass;
import static com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase.getSubAnnotationListForAnnotationsOfClassType;
import static com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase.getSubAnnotationListForElementsOfOwnerClass;
import static com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase.getSubAnnotationListUsingExclusionFilters;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the logic in {@link ReflectionBasedJsr303AnnotationTrollerBase} to make sure that its reflection magic works the way we expect it to.
 *
 * @author Nic Munroe
 */
public class ReflectionMagicWorksTest {
    // This is static to make sure that it only gets created once in a JUnit context (rather than once per test method) because this is potentially time consuming.
    private static final ReflectionBasedJsr303AnnotationTrollerBase TROLLER = new ReflectionBasedJsr303AnnotationTrollerBase() {
        @Override
        protected List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses() {
            return null;
        }

        @Override
        protected List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject() {
            return null;
        }
    };

    private static final List<Predicate<Pair<Annotation, AnnotatedElement>>> STRICT_MEMBER_CHECK_EXCLUSIONS = StrictMemberCheck.getStrictMemberCheckExclusionsForNonCompliantDeclarations();

    // ============== THE FOLLOWING TESTS VERIFY THAT ReflectionBasedJsr303AnnotationTrollerBase's CRAZY REFLECTION MAGIC STUFF WORKS AS EXPECTED. ========================
    /**
     * Makes sure that the Reflections helper stuff is working properly and capturing all annotation possibilities (class type, constructor, constructor param, method, method param, and field).
     */
    @Test
    public void verifyThatTheReflectionsConfigurationIsCapturingAllAnnotationPossibilities() {
        List<Pair<Annotation, AnnotatedElement>> annotationOptionsClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                DifferentValidationAnnotationOptions.class);
        assertThat(annotationOptionsClassAnnotations.size()).isEqualTo(10);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, SomeClassLevelJsr303Annotation.class)).hasSize(2);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, OtherClassLevelJsr303Annotation.class)).hasSize(1);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, AssertTrue.class)).hasSize(1);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, AssertFalse.class)).hasSize(1);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, NotNull.class)).hasSize(2);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, Min.class)).hasSize(2);
        assertThat(getSubAnnotationListForAnnotationsOfClassType(annotationOptionsClassAnnotations, Max.class)).hasSize(1);
    }

    /**
     * Verifies that {@link ReflectionBasedJsr303AnnotationTrollerBase#getSubAnnotationListUsingExclusionFilters(java.util.List, java.util.List, java.util.List)} helper method for this test class is working properly when passing
     * in annotatedElementOwnerClassesToExclude exclusion filter.
     */
    @Test
    public void verifyThatExclusionFilterMethodIsExcludingSpecifiedClasses() {
        List<Pair<Annotation, AnnotatedElement>> annotationOptionsClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                DifferentValidationAnnotationOptions.class);
        List<Pair<Annotation, AnnotatedElement>> strictMemberCheckClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                StrictMemberCheck.class);

        List<Pair<Annotation, AnnotatedElement>> combinedAnnotations = new ArrayList<>(annotationOptionsClassAnnotations);
        combinedAnnotations.addAll(strictMemberCheckClassAnnotations);

        assertThat(getSubAnnotationListUsingExclusionFilters(combinedAnnotations, singletonList(DifferentValidationAnnotationOptions.class), null))
            .hasSize(strictMemberCheckClassAnnotations.size());
        assertThat(getSubAnnotationListUsingExclusionFilters(combinedAnnotations, singletonList(StrictMemberCheck.class), null))
            .hasSize(annotationOptionsClassAnnotations.size());
        assertThat(getSubAnnotationListUsingExclusionFilters(combinedAnnotations, Arrays.asList(DifferentValidationAnnotationOptions.class, StrictMemberCheck.class), null))
            .isEmpty();
    }

    /**
     * Another test for {@link ReflectionBasedJsr303AnnotationTrollerBase#getSubAnnotationListUsingExclusionFilters(java.util.List, java.util.List, java.util.List)}, this time for the specificAnnotationDeclarationExclusionMatchers
     * exclusion filter.
     */
    @Test
    public void verifyThatExclusionFilterMethodIsExcludingSpecifiedMembers() {
        List<Pair<Annotation, AnnotatedElement>> strictMemberCheckClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                StrictMemberCheck.class);

        assertThat(strictMemberCheckClassAnnotations.size()).isGreaterThan(4);
        assertThat(getSubAnnotationListUsingExclusionFilters(strictMemberCheckClassAnnotations, null, STRICT_MEMBER_CHECK_EXCLUSIONS))
            .hasSize(strictMemberCheckClassAnnotations.size() - 4);
    }

    /**
     * Another test for {@link ReflectionBasedJsr303AnnotationTrollerBase#getSubAnnotationListUsingExclusionFilters(java.util.List, java.util.List, java.util.List)}
     */
    @Test
    public void verifyThatExclusionFilterMethodIsExcludingBoth() {
        List<Pair<Annotation, AnnotatedElement>> annotationOptionsClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                DifferentValidationAnnotationOptions.class);
        List<Pair<Annotation, AnnotatedElement>> strictMemberCheckClassAnnotations = getSubAnnotationListForElementsOfOwnerClass(TROLLER.allConstraintAnnotationsMasterList,
                StrictMemberCheck.class);

        List<Pair<Annotation, AnnotatedElement>> combinedAnnotations = new ArrayList<>(annotationOptionsClassAnnotations);
        combinedAnnotations.addAll(strictMemberCheckClassAnnotations);

        assertTrue(strictMemberCheckClassAnnotations.size() > 4);
        assertThat(
            getSubAnnotationListUsingExclusionFilters(
                strictMemberCheckClassAnnotations, singletonList(DifferentValidationAnnotationOptions.class), STRICT_MEMBER_CHECK_EXCLUSIONS
            )
        ).hasSize(strictMemberCheckClassAnnotations.size() - 4);
    }

    @SomeClassLevelJsr303Annotation.List(
            {
                    @SomeClassLevelJsr303Annotation(message = "I am a class annotated with a constraint in a list 1"),
                    @SomeClassLevelJsr303Annotation(message = "I am a class annotated with a constraint in a list 2")
            }
    )
    @OtherClassLevelJsr303Annotation(message = "I am a class annotated with a constraint NOT in a list")
    @SuppressWarnings("unused")
    public static class DifferentValidationAnnotationOptions {
        @Min(value = 1, message = "I am a field annotated with a constraint")
        private Integer annotatedField;

        @AssertTrue(message = "I am a constructor annotated with a constraint even though it doesn't really make sense")
        public DifferentValidationAnnotationOptions(String nonAnnotatedConstructorParam,
                                                    @NotNull(message = "I am a constructor param annotated with a constraint 1") String annotatedConstructorParam1,
                                                    @NotNull(message = "I am a constructor param annotated with a constraint 2") String annotatedConstructorParam2,
                                                    String alsoNotAnnotatedConstructorParam) {

        }

        @AssertFalse(message = "I am an annotated method")
        public boolean annotatedMethod(String nonAnnotatedMethodParam,
                                       @Max(value = 42, message = "I am an annotated method param 1") Integer annotatedMethodParam1,
                                       @Min(value = 42, message = "I am an annotated method param 2") Integer annotatedMethodParam2,
                                       String alsoNotAnnotatedMethodParam) {
            return true;
        }
    }

    @SomeClassLevelJsr303Annotation.List(
            {
                    @SomeClassLevelJsr303Annotation(message = "I am not a ApiError enum name - class annotation"),
                    @SomeClassLevelJsr303Annotation(message = "INVALID_COUNT_VALUE")
            }
    )
    @OtherClassLevelJsr303Annotation(message = "I am also not a ApiError enum name - class annotation 2")
    @SuppressWarnings("unused")
    public static class StrictMemberCheck {
        @Min(value = 1, message="INVALID_COUNT_VALUE")
        public Integer compliantField;
        @Min(value = 1, message="I am not a ApiError enum name - field annotation")
        private Integer nonCompliantField;

        @NotNull(message = "TYPE_CONVERSION_ERROR")
        private String compliantMethod() {
            return null;
        }

        @NotNull(message = "I am not a ApiError enum name - method annotation")
        public String nonCompliantMethod() {
            return null;
        }

        /**
         * @return The list of annotation exclusions (generated by {@link ReflectionBasedJsr303AnnotationTrollerBase#generateExclusionForAnnotatedElementAndAnnotationClass(java.lang.reflect.AnnotatedElement, Class)})
         *          for this StrictMemberCheck class that are intentionally not compliant with the "all JSR 303 messages should point to {@link com.nike.backstopper.apierror.ApiError}" requirement
         *          (and should therefore not cause the unit tests based on {@link ReflectionBasedJsr303AnnotationTrollerBase} to fail).
         */
        public static List<Predicate<Pair<Annotation, AnnotatedElement>>> getStrictMemberCheckExclusionsForNonCompliantDeclarations() {
            try {
                List<Predicate<Pair<Annotation, AnnotatedElement>>> strictExclusionsList = new ArrayList<>();

                //noinspection CollectionAddAllCanBeReplacedWithConstructor
                strictExclusionsList.addAll(
                        Arrays.asList(generateExclusionForAnnotatedElementAndAnnotationClass(StrictMemberCheck.class.getDeclaredField("nonCompliantField"), Min.class),
                                generateExclusionForAnnotatedElementAndAnnotationClass(StrictMemberCheck.class.getDeclaredMethod("nonCompliantMethod"), NotNull.class),
                                generateExclusionForAnnotatedElementAndAnnotationClass(StrictMemberCheck.class, OtherClassLevelJsr303Annotation.class),
                                generateExclusionForAnnotatedElementAndAnnotationClass(StrictMemberCheck.class, SomeClassLevelJsr303Annotation.class).and(
                                       input -> {
                                           // At this point we know it's StrictMemberCheck class and a SomeClassLevelJsr303Annotation annotation.
                                           //       We want to exclude the one we know is bad that we don't care about.
                                           String message = extractMessageFromAnnotation(input.getLeft());
                                           return message.equals("I am not a ApiError enum name - class annotation");
                                       }
                                )));

                return strictExclusionsList;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = SomeClassLevelJsr303AnnotationValidator.class)
    @SuppressWarnings("unused")
    public @interface SomeClassLevelJsr303Annotation {
        String message() default "{SomeClassLevelJsr303Annotation.message}";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};

        @Target({TYPE, ANNOTATION_TYPE})
        @Retention(RUNTIME)
        @Documented
        @interface List {
            SomeClassLevelJsr303Annotation[] value();
        }
    }

    public static class SomeClassLevelJsr303AnnotationValidator implements ConstraintValidator<SomeClassLevelJsr303Annotation, Object> {
        @Override
        public void initialize(SomeClassLevelJsr303Annotation constraintAnnotation) {

        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return true;
        }
    }

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = OtherClassLevelJsr303AnnotationValidator.class)
    @SuppressWarnings("unused")
    public @interface OtherClassLevelJsr303Annotation {
        String message() default "{OtherClassLevelJsr303Annotation.message}";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};

        @Target({TYPE, ANNOTATION_TYPE})
        @Retention(RUNTIME)
        @Documented
        @interface List {
            OtherClassLevelJsr303Annotation[] value();
        }
    }

    public static class OtherClassLevelJsr303AnnotationValidator implements ConstraintValidator<OtherClassLevelJsr303Annotation, Object> {
        @Override
        public void initialize(OtherClassLevelJsr303Annotation constraintAnnotation) {

        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return true;
        }
    }

}
