package jsr303convention;

import com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase;
import com.nike.internal.util.Pair;

import com.google.common.base.Predicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

/**
 * Extension of {@link ReflectionBasedJsr303AnnotationTrollerBase} used by {@link VerifyJsr303ContractTest} and
 * {@link VerifyStringConvertsToClassTypeAnnotationsAreValidTest}).
 */
public final class ApplicationJsr303AnnotationTroller extends ReflectionBasedJsr303AnnotationTrollerBase {

    private static final ApplicationJsr303AnnotationTroller INSTANCE = new ApplicationJsr303AnnotationTroller();

    @SuppressWarnings("WeakerAccess")
    public static ApplicationJsr303AnnotationTroller getInstance() {
        return INSTANCE;
    }

    // Intentionally private - use {@code getInstance()} to retrieve the singleton instance of this class.
    private ApplicationJsr303AnnotationTroller() {
        super();
    }

    @Override
    protected List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses() {
        return null;
    }

    @Override
    protected List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject() {
        return null;
    }
}
