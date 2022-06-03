package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.internal.util.Pair;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that *ALL* non-excluded JSR 303 validation annotations in this project have a message defined that maps to a
 * {@link ApiError#getName()} for one of the errors found in the project's {@link #getProjectApiErrors()}.
 * You can exclude annotation declarations by making sure that the {@link #getAnnotationTroller()} you use has populated
 * its {@link ReflectionBasedJsr303AnnotationTrollerBase#ignoreAllAnnotationsAssociatedWithTheseClasses} and {@link
 * ReflectionBasedJsr303AnnotationTrollerBase#specificAnnotationDeclarationsExcludedFromStrictMessageRequirement} lists
 * appropriately.
 *
 * @deprecated This is the JUnit 4 version and will not be maintained long term. Please migrate to the JUnit 5 module: backstopper-reusable-tests-junit5
 * @author Nic Munroe
 * @see com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase
 */
@Deprecated
public abstract class VerifyJsr303ValidationMessagesPointToApiErrorsTest {

    /**
     * @return The annotation troller to use for your project. This should likely be accessed as a singleton - see the
     * javadocs for {@link ReflectionBasedJsr303AnnotationTrollerBase} for more info on why and example code on how to
     * do it.
     */
    protected abstract ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller();

    /**
     * @return The {@link ProjectApiErrors} for your project.
     */
    protected abstract ProjectApiErrors getProjectApiErrors();

    /**
     * Makes sure that any constraint annotation messages that aren't explicitly excluded point to an {@link
     * com.nike.backstopper.apierror.ApiError} name from your project's {@link #getProjectApiErrors()}.
     */
    @Test
    public void verifyThatAllValidationAnnotationsReferToApiErrors()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final ReflectionBasedJsr303AnnotationTrollerBase troller = getAnnotationTroller();
        List<Pair<Annotation, AnnotatedElement>> relevantAnnotations =
            troller.projectRelevantConstraintAnnotationsExcludingUnitTestsList;

        List<InvalidAnnotationDescription> invalidAnnotations = new ArrayList<>();
        for (Pair<Annotation, AnnotatedElement> pair : relevantAnnotations) {
            Annotation annotation = pair.getLeft();
            AnnotatedElement annotatedElement = pair.getRight();
            String message = ReflectionBasedJsr303AnnotationTrollerBase.extractMessageFromAnnotation(annotation);
            ApiError apiError;
            try {
                apiError = getProjectApiErrors().convertToApiError(message);
                assertThat(apiError, notNullValue());
            }
            catch (Throwable ex) {
                // This constraint annotation has an invalid message value. Keep track of it for later so we can spit
                //      all the invalid ones out at once.
                invalidAnnotations.add(new InvalidAnnotationDescription(annotation, annotatedElement, message));
            }
        }

        if (invalidAnnotations.size() > 0) {
            // We have at least one invalid annotation, so this unit test will need to fail.
            // Sort our invalid-annotations list to make it easier to fix errors for the developer looking at the error output.
            Collections.sort(invalidAnnotations, new Comparator<InvalidAnnotationDescription>() {
                @Override
                public int compare(InvalidAnnotationDescription o1, InvalidAnnotationDescription o2) {
                    int classComparison = ReflectionBasedJsr303AnnotationTrollerBase
                        .getOwnerClass(o1.annotatedElement)
                        .getName()
                        .compareTo(ReflectionBasedJsr303AnnotationTrollerBase.getOwnerClass(o2.annotatedElement).getName());

                    if (classComparison != 0)
                        return classComparison;

                    return ReflectionBasedJsr303AnnotationTrollerBase
                        .getAnnotatedElementLocationAsString(o1.annotatedElement)
                        .compareTo(
                            ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(o2.annotatedElement)
                        );
                }
            });

            // Generate a giant error output message containing all the invalid annotations and instructions on how to deal with them.
            StringBuilder sb = new StringBuilder();
            sb.append("There are ").append(invalidAnnotations.size())
              .append( " JSR 303 validation annotations that are invalid. All validation annotations MUST contain a "
                       + "message and that message MUST map to one of the ApiError names contained in ")
              .append(getProjectApiErrors().getClass().getName())
              .append(". If any of these are false positive errors then you must add the Class or specific "
                      + "Member/annotated element that owns the annotation to one of the exclusion lists in this unit test.\n")
              .append("You should only exclude annotations, however, if you REALLY REALLY REALLY know what you're doing "
                      + "and can 100% GUARANTEE that the exclusion won't break your error handling contract, because "
                      + "this strict message requirement *IS* how we're guaranteeing your error handling contract.\n")
              .append("Here are the invalid annotations, where they are found, and the offending message:")
              .append("\nANNOTATION CLASS\t|\tLOCATION\t|\tMESSAGE");
            for (InvalidAnnotationDescription invalidAnnotation : invalidAnnotations) {
                AnnotatedElement annotatedElement = invalidAnnotation.annotatedElement;
                sb.append("\n@").append(invalidAnnotation.annotation.annotationType().getSimpleName()).append("\t|\t");

                sb.append(ReflectionBasedJsr303AnnotationTrollerBase
                              .getAnnotatedElementLocationAsString(annotatedElement));

                sb.append("\t|\t")
                  .append(invalidAnnotation.message);
            }
            // Fail the unit test with our custom giant error message.
            throw new AssertionError(sb.toString());
        }
    }

    /**
     * DTO class describing the context of an invalid annotation.
     */
    @SuppressWarnings("WeakerAccess")
    private static class InvalidAnnotationDescription {

        public final Annotation annotation;
        public final AnnotatedElement annotatedElement;
        public final String message;

        private InvalidAnnotationDescription(Annotation annotation, AnnotatedElement annotatedElement, String message) {
            this.annotation = annotation;
            this.annotatedElement = annotatedElement;
            this.message = message;
        }
    }
}
