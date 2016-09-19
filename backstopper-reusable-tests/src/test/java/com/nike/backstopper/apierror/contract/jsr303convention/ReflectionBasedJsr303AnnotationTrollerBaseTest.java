package com.nike.backstopper.apierror.contract.jsr303convention;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Mainly here to get code coverage for {@link ReflectionBasedJsr303AnnotationTrollerBase} above and beyond what
 * {@link ReflectionMagicWorksTest} does.
 *
 * @author Nic Munroe
 */
public class ReflectionBasedJsr303AnnotationTrollerBaseTest {

    @Test
    public void getAnnotatedElementLocationAsString_adds_correct_info_for_constructor() throws NoSuchMethodException {
        // given
        Constructor constructor = TestClass.class.getConstructor();

        // when
        String result = ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(constructor);

        // then
        assertThat(result).isEqualTo(TestClass.class.getName() + "[CONSTRUCTOR]");
    }

    @Test
    public void getAnnotatedElementLocationAsString_adds_correct_info_for_class() throws NoSuchMethodException {
        // given
        Class clazz = TestClass.class;

        // when
        String result = ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(clazz);

        // then
        assertThat(result).isEqualTo(TestClass.class.getName() + "[CLASS]");
    }

    @Test
    public void getAnnotatedElementLocationAsString_adds_correct_info_for_method() throws NoSuchMethodException {
        // given
        Method method = TestClass.class.getDeclaredMethod("fooMethod");

        // when
        String result = ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(method);

        // then
        assertThat(result).isEqualTo(TestClass.class.getName() + "." + method.getName() + "[METHOD]");
    }

    @Test
    public void getAnnotatedElementLocationAsString_adds_correct_info_for_field()
        throws NoSuchMethodException, NoSuchFieldException {
        // given
        Field field = TestClass.class.getDeclaredField("fooField");

        // when
        String result = ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(field);

        // then
        assertThat(result).isEqualTo(TestClass.class.getName() + "." + field.getName() + "[FIELD]");
    }

    @Test
    public void getAnnotatedElementLocationAsString_adds_correct_info_for_unknown_AnnotatedElement()  {
        // given
        AnnotatedElement oddThing = new OddThing();

        // when
        String result = ReflectionBasedJsr303AnnotationTrollerBase.getAnnotatedElementLocationAsString(oddThing);

        // then
        assertThat(result).isEqualTo(OddThing.class.getName() + "." + oddThing.toString() + "[???]");
    }

    @Test
    public void getOwnerClass_throws_IllegalArgumentException_if_AnnotatedElement_is_not_Member_or_Class() {
        // given
        final AnnotatedElement notMemberOrClass = mock(AnnotatedElement.class);

        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                ReflectionBasedJsr303AnnotationTrollerBase.getOwnerClass(notMemberOrClass);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void extractMessageFromAnnotation_throws_wrapped_RuntimeException_if_annotation_blows_up() {
        // given
        RuntimeException exToThrow = new RuntimeException("kaboom");
        final Annotation annotation = mock(Annotation.class);
        doThrow(exToThrow).when(annotation).annotationType();

        // when
        Throwable actual = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                ReflectionBasedJsr303AnnotationTrollerBase.extractMessageFromAnnotation(annotation);
            }
        });

        // then
        assertThat(actual)
            .isNotEqualTo(exToThrow)
            .isInstanceOf(RuntimeException.class)
            .hasCause(exToThrow);
    }

    private static class TestClass {

        public String fooField = "fooField";

        public TestClass() {}

        public String fooMethod() {
            return "fooMethod";
        }
    }

    private static class OddThing implements AnnotatedElement, Member {

        public final String toStringVal = UUID.randomUUID().toString();

        @Override
        public String toString() {
            return toStringVal;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return OddThing.class;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public boolean isSynthetic() {
            return false;
        }
    }
}