package test;

import com.google.testing.compile.Compiler;
import com.nike.backstopper.apierror.ApiErrorValue;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test class for testing the functionality of {@link ApiErrorValueProcessor} using {@link Compiler#javac()}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueWrongModelCompileTimeTest {

    @ApiErrorValue(httpStatusCode = -1)
    @NotNull(message = "message")
    private String filedWithNegativeHttpStatusCode;

    @ApiErrorValue(errorCode = "")
    @NotNull(message = "message")
    private String filedWithEmptyErrorCode;

    @ApiErrorValue(errorCode = "errorCode")
    @NotNull(message = "")
    private String filedWithEmptyMessage;

    @ApiErrorValue(errorCode = " ")
    @NotNull(message = "message")
    private String filedWithBlankErrorCode;

    @ApiErrorValue(errorCode = "errorCode")
    @NotNull(message = " ")
    private String filedWithBlankMessage;

    @ApiErrorValue(errorCode = "errorCode")
    private String filedWithoutValidationAnnotation;

    @ApiErrorValue(errorCode = "errorCode")
    @CustomConstraintValidationWithoutMessageAnnotation
    private String filedWithCustomConstraintValidationWithoutMessageAnnotation;

    @ApiErrorValue(errorCode = "errorCode")
    @CustomConstraintValidationWithoutConstraintAnnotation
    private String filedWithCustomConstraintValidationWithoutConstraintAnnotation;

    @ApiErrorValue(errorCode = "errorCode")
    @NotNull(message = "message")
    // inherit @ApiErrorValue, should be skipped.
    @CustomDelegatedValidationAnnotation
    private String filedWithDuplicatedApiErrorValue;

    @Constraint(validatedBy = CustomConstraintValidationWithoutMessageAnnotationImpl.class)
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomConstraintValidationWithoutMessageAnnotation {

        // message method should be present.
        //String message() default "CustomConstraintValidationWithoutConstraintAnnotation default message";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }

    private static class CustomConstraintValidationWithoutMessageAnnotationImpl
            implements ConstraintValidator<CustomConstraintValidationWithoutMessageAnnotation, String> {

        @Override
        public void initialize(CustomConstraintValidationWithoutMessageAnnotation constraintAnnotation) {
            // do nothing.
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && !value.isEmpty();
        }

    }

    // @Constraint annotation should be present.
    //@Constraint(validatedBy = CustomConstraintValidationWithoutConstraintAnnotationImpl.class)
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomConstraintValidationWithoutConstraintAnnotation {

        String message() default "CustomConstraintValidationWithoutConstraintAnnotation default message";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }

    private static class CustomConstraintValidationWithoutConstraintAnnotationImpl
            implements ConstraintValidator<CustomConstraintValidationWithoutConstraintAnnotation, String> {

        @Override
        public void initialize(CustomConstraintValidationWithoutConstraintAnnotation constraintAnnotation) {
            // do nothing.
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && !value.isEmpty();
        }

    }

    @ApiErrorValue
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CustomDelegatedValidationAnnotation { }

}
