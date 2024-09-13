package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.internal.util.Pair;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests basic functionality of {@link VerifyJsr303ValidationMessagesPointToApiErrorsTest}.
 *
 * @author Nic Munroe
 */
public class VerifyJsr303ValidationMessagesPointToApiErrorsTestTest {

    @Test
    public void verify_basic_functionality() {
        // given
        final VerifyJsr303ValidationMessagesPointToApiErrorsTest tester = new VerifyJsr303ValidationMessagesPointToApiErrorsTest() {
            @Override
            protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
                return new ReflectionBasedJsr303AnnotationTrollerBase() {
                    @Override
                    protected List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses() {
                        return Arrays.asList(
                            ReflectionMagicWorksTest.DifferentValidationAnnotationOptions.class,
                            ReflectionMagicWorksTest.StrictMemberCheck.class
                        );
                    }

                    @Override
                    protected List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject() {
                        return null;
                    }
                };
            }

            @Override
            protected ProjectApiErrors getProjectApiErrors() {
                return ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
            }
        };

        // when
        Throwable ex = catchThrowable(tester::verifyThatAllValidationAnnotationsReferToApiErrors);

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("GARBAGE")
            .hasMessageContaining("NOT_A_THING");
        assertThat(ex.getMessage()).doesNotContain("GENERIC_BAD_REQUEST");
    }

    @SuppressWarnings("unused")
    private static class TestValidationObject {

        @NotNull(message = "GENERIC_BAD_REQUEST") // This one should not trigger a unit test error since it exists in the ProjectApiErrors
        public String fooString;

        @Min(message = "GARBAGE", value = 0)
        public Integer fooInteger;

        @NotNull(message = "NOT_A_THING")
        public Object fooObject;
    }

}