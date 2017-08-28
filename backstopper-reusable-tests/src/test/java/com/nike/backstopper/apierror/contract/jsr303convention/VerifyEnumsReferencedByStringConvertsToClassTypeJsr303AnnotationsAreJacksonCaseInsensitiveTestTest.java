package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Predicate;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

/**
 * Tests basic functionality of {@link VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest}.
 *
 * @author Nic Munroe
 */
public class VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTestTest {

    @Test
    public void verify_test_passes_for_valid_annotations_and_enum_definitions() {
        // given
        final VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest testImpl =
            getTestImpl("caseSensitiveEnumFieldWithInsensitivityAllowed");

        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                testImpl.verifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreCaseInsensitive();
            }
        });

        // then
        assertThat(ex).isNull();
    }

    @Test
    public void verify_test_fails_for_annotation_that_allows_case_insensitivity_but_enum_that_is_case_sensitive() {
        // given
        final VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest testImpl =
            getTestImpl();

        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                testImpl.verifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreCaseInsensitive();
            }
        });

        // then
        assertThat(ex)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("$CaseSensitiveEnum")
            .hasMessageContaining("AnnotatedClass.caseSensitiveEnumFieldWithInsensitivityAllowed");
    }

    private VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest
                getTestImpl(final String ... fieldsToIgnore) {
        return new VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest() {
            @Override
            protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
                return new ReflectionBasedJsr303AnnotationTrollerBase() {
                    @Override
                    protected List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses() {
                        return null;
                    }

                    @Override
                    protected List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject()
                        throws Exception {
                        if (fieldsToIgnore == null || fieldsToIgnore.length == 0)
                            return null;

                        List<Predicate<Pair<Annotation, AnnotatedElement>>> ignoreList = new ArrayList<>();
                        for (String fieldName : fieldsToIgnore) {
                            ignoreList.add(
                                ReflectionBasedJsr303AnnotationTrollerBase
                                    .generateExclusionForAnnotatedElementAndAnnotationClass(
                                        AnnotatedClass.class.getDeclaredField(fieldName), StringConvertsToClassType.class)
                            );
                        }
                        return ignoreList;
                    }
                };
            }
        };
    }

    private enum CaseSensitiveEnum {
        foo, BAR, BlAh
    }

    private enum CaseInsensitiveEnum {
        BAZ, bat, wHeE;

        @JsonCreator
        public static CaseInsensitiveEnum toCaseInsensitiveEnum(String stringVal) {
            for (CaseInsensitiveEnum enumVal : values()) {
                if (enumVal.name().equalsIgnoreCase(stringVal))
                    return enumVal;
            }
            throw new IllegalArgumentException("Cannot convert the string: \"" + stringVal + "\" to a valid CaseInsensitiveEnum enum value.");
        }
    }

    private static class AnnotatedClass {
        @StringConvertsToClassType(message = "GENERIC_BAD_REQUEST", classType = CaseSensitiveEnum.class, allowCaseInsensitiveEnumMatch = true)
        public String caseSensitiveEnumFieldWithInsensitivityAllowed;

        @StringConvertsToClassType(message = "GENERIC_BAD_REQUEST", classType = CaseSensitiveEnum.class, allowCaseInsensitiveEnumMatch = false)
        public String caseSensitiveEnumFieldWithInsensitivityDisallowed;

        @StringConvertsToClassType(message = "GENERIC_BAD_REQUEST", classType = CaseInsensitiveEnum.class, allowCaseInsensitiveEnumMatch = true)
        public String caseInsensitiveEnumFieldWithInsensitivityAllowed;

        @StringConvertsToClassType(message = "GENERIC_BAD_REQUEST", classType = CaseInsensitiveEnum.class, allowCaseInsensitiveEnumMatch = false)
        public String caseInsensitiveEnumFieldWithInsensitivityDisallowed;
    }
}