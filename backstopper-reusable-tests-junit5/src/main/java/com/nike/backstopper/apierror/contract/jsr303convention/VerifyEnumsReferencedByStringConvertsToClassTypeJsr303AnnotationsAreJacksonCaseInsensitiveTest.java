package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes sure that any Enums referenced by {@link StringConvertsToClassType} JSR 303 annotations are case insensitive
 * if {@link StringConvertsToClassType#allowCaseInsensitiveEnumMatch()} is true when being deserialized
 * (e.g. by Jackson).
 *
 * <p>This test is only necessary if you are using {@link StringConvertsToClassType} annotations for validation, *and*
 * you want to support case-insensitive enum values during deserialization.
 *
 * <p>You can exclude annotation declarations by making sure that the {@link #getAnnotationTroller()} you use has
 * populated its {@code ReflectionBasedJsr303AnnotationTrollerBase.ignoreAllAnnotationsAssociatedWithTheseClasses} and
 * {@code ReflectionBasedJsr303AnnotationTrollerBase.specificAnnotationDeclarationsExcludedFromStrictMessageRequirement}
 * lists appropriately.
 *
 * @author Nic Munroe
 * @see com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase
 */
public abstract class VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest {

    // This field, and specifically the field annotation, is required to fix a strange behavior in the org.reflections
    //      stuff. If we don't have this, then test might fail deep in the guts of org.reflections due to an exception
    //      that looks like:
    //          Caused by: org.reflections.ReflectionsException: Scanner FieldAnnotationsScanner was not configured
    //
    //      Putting this field here means the scanner will find at least one match for FieldAnnotationsScanner, which
    //      will in turn cause it to be configured.
    @SuppressWarnings("unused")
    private final @Nullable String fixReflectionsFieldScanner = null;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return The annotation troller to use for your project. This should likely be accessed as a singleton - see the
     * javadocs for {@link ReflectionBasedJsr303AnnotationTrollerBase} for more info on why and example code on how to
     * do it.
     */
    protected abstract ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller();

    /**
     * Makes sure that any enums referenced by {@link StringConvertsToClassType} annotations in your project where
     * {@link StringConvertsToClassType#allowCaseInsensitiveEnumMatch()} is true (and that aren't explicitly excluded)
     * support case insensitive deserialization when being deserialized by Jackson. See the javadocs for
     * {@link StringConvertsToClassType} for more info on why this is required and how to do it.
     */
    @Test
    @SuppressWarnings("ExtractMethodRecommender")
    public void verifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreCaseInsensitive()
        throws IOException {
        ReflectionBasedJsr303AnnotationTrollerBase troller = getAnnotationTroller();
        List<Pair<Annotation, AnnotatedElement>> allStringConvertsToClassTypeAnnotations =
            ReflectionBasedJsr303AnnotationTrollerBase.getSubAnnotationListForAnnotationsOfClassType(
                troller.projectRelevantConstraintAnnotationsExcludingUnitTestsList, StringConvertsToClassType.class);

        for (Pair<Annotation, AnnotatedElement> annotationPair : allStringConvertsToClassTypeAnnotations) {
            StringConvertsToClassType sctctAnnotation = (StringConvertsToClassType) annotationPair.getLeft();
            if (sctctAnnotation.classType().isEnum() && sctctAnnotation.allowCaseInsensitiveEnumMatch()) {
                // This field is supposed to be able to deserialize to the desired enum in a case insensitive way.
                //      Make sure the enum supports case insensitive deserialization.
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) sctctAnnotation.classType();

                // Grab all the enum values for the enum referenced by this @StringConvertsToClassType declaration.
                Object[] enumValuesArray = sctctAnnotation.classType().getEnumConstants();
                assertThat(enumValuesArray).isNotNull().isNotEmpty();

                // For each enum value, verify that we can use Jackson to serialize it, convert it to an alternate case,
                //      and then deserialize the alternate version successfully.
                for (Object enumValue : enumValuesArray) {
                    String enumAsJsonString = objectMapper.writeValueAsString(enumValue);
                    String lowercaseEnumAsJsonString = enumAsJsonString.toLowerCase();
                    String uppercaseEnumAsJsonString = enumAsJsonString.toUpperCase();

                    // Verify that the lowercase version really is different - if it's the same then do uppercase
                    //      instead.
                    String alternateCaseEnumAsJsonString = enumAsJsonString.equals(lowercaseEnumAsJsonString)
                                                           ? uppercaseEnumAsJsonString
                                                           : lowercaseEnumAsJsonString;
                    // Sanity check that the alternate case really is different than the original enum's name.
                    assertThat(alternateCaseEnumAsJsonString).isNotEqualTo(enumAsJsonString);

                    boolean deserializationSucceeded;
                    try {
                        Enum<?> deserializedEnum = objectMapper.readValue(alternateCaseEnumAsJsonString, enumClass);
                        deserializationSucceeded = deserializedEnum.equals(enumValue);
                    }
                    catch (Throwable ex) {
                        deserializationSucceeded = false;
                    }

                    if (!deserializationSucceeded) {
                        @SuppressWarnings("StringBufferReplaceableByString")
                        StringBuilder sb = new StringBuilder();
                        sb.append("Found a @").append(StringConvertsToClassType.class.getSimpleName())
                          .append(" annotation that references an enum class type that is not case insensitive. ")
                          .append( "This most likely means you need to add a @JsonCreator method in the enum that knows "
                                   + "how to deserialize the enum in a case insensitive manner. ")
                          .append("Offending enum class: ").append(enumClass.getName())
                          .append(", offending element containing the annotation: ")
                          .append(ReflectionBasedJsr303AnnotationTrollerBase
                                      .getAnnotatedElementLocationAsString(annotationPair.getRight()));
                        throw new AssertionError(sb.toString());
                    }
                }
            }
        }
    }
}
