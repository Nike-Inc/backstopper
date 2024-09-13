package jsr303convention;

import com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase;
import com.nike.backstopper.apierror.contract.jsr303convention.VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest;
import com.nike.backstopper.validation.constraints.StringConvertsToClassType;

/**
 * Makes sure that any Enums referenced by {@link StringConvertsToClassType} JSR 303 annotations are case insensitive if
 * they are marked with {@link StringConvertsToClassType#allowCaseInsensitiveEnumMatch()} set to true.
 */
public class VerifyStringConvertsToClassTypeAnnotationsAreValidTest
    extends VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest {

    @Override
    protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
        return ApplicationJsr303AnnotationTroller.getInstance();
    }
}
