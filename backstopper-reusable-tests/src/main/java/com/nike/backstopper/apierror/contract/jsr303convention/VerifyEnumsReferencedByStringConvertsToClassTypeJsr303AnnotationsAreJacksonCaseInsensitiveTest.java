package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Makes sure that any Enums referenced by {@link StringConvertsToClassType} JSR 303 annotations are case insensitive
 * if {@link StringConvertsToClassType#allowCaseInsensitiveEnumMatch()} is true when being deserialized
 * (e.g. by Jackson).
 *
 * <p>This test is only necessary if you are using {@link StringConvertsToClassType} annotations for validation, *and*
 * you want to support case-insensitive enum values during deserialization.
 *
 * <p>You can exclude annotation declarations by making sure that the {@link #getAnnotationTroller()} you use has
 * populated its {@link ReflectionBasedJsr303AnnotationTrollerBase#ignoreAllAnnotationsAssociatedWithTheseClasses} and
 * {@link ReflectionBasedJsr303AnnotationTrollerBase#specificAnnotationDeclarationsExcludedFromStrictMessageRequirement}
 * lists appropriately.
 *
 * @deprecated This is the JUnit 4 version and will not be maintained long term. Please migrate to the JUnit 5 module: backstopper-reusable-tests-junit5
 * @author Nic Munroe
 * @see com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase
 */
@Deprecated
public abstract class VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

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
                Class<? extends Enum> enumClass = (Class<? extends Enum>) sctctAnnotation.classType();

                // Grab all the enum values for the enum referenced by this @StringConvertsToClassType declaration.
                Object[] enumValuesArray = sctctAnnotation.classType().getEnumConstants();
                assertThat(enumValuesArray, notNullValue());

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
                    assertThat(alternateCaseEnumAsJsonString, not(enumAsJsonString));

                    boolean deserializationSucceeded;
                    try {
                        Enum deserializedEnum = objectMapper.readValue(alternateCaseEnumAsJsonString, enumClass);
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
