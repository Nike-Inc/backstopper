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
public class ApiErrorValueModelCompileTimeTest {

    @ApiErrorValue(errorCode = "validatedByField errorCode", httpStatusCode = 401)
    @NotNull(message = "validatedByField message")
    private String validatedByField;
    @ApiErrorValue
    @NotNull(message = "validatedByFieldWithDefaultValues message")
    private Integer validatedByFieldWithDefaultValues;

    private String validatedByConstructor;
    private Integer validatedByConstructorWithDefaultValues;

    private String validatedByMethod;
    private Integer validatedByMethodWithDefaultValues;

    private String validatedByParameter;
    private Integer validatedByParameterWithDefaultValues;

    @CustomConstraintValidationAnnotation(message = "validatedByCustomConstraintValidationAnnotation message")
    @ApiErrorValue(errorCode = "validatedByCustomConstraintValidationAnnotation errorCode", httpStatusCode = 401)
    private String validatedByCustomConstraintValidationAnnotation;
    @CustomConstraintValidationAnnotation
    @ApiErrorValue
    private String validatedByCustomConstraintValidationAnnotationWithDefaultValues;

    @CustomDelegatedValidationAnnotation
    private String validatedByCustomDelegatedValidationAnnotation;
    @CustomDelegatedValidationAnnotationWithDefaultValues
    private String validatedByCustomDelegatedValidationAnnotationWithDefaultValues;

    @ApiErrorValue(errorCode = "validatedByConstructor errorCode", httpStatusCode = 401)
    @NotNull(message = "validatedByConstructor message")
    public ApiErrorValueModelCompileTimeTest(String validatedByConstructor) {
        this.validatedByConstructor = validatedByConstructor;
    }

    @ApiErrorValue
    @NotNull(message = "validatedByConstructorWithDefaultValues message")
    public ApiErrorValueModelCompileTimeTest(Integer validatedByConstructorWithDefaultValues) {
        this.validatedByConstructorWithDefaultValues = validatedByConstructorWithDefaultValues;
    }


    @ApiErrorValue(errorCode = "validatedByMethod errorCode", httpStatusCode = 401)
    @NotNull(message = "validatedByMethod message")
    private void setValidatedByMethod(String validatedByMethod) {
        this.validatedByMethod = validatedByMethod;
    }

    @ApiErrorValue
    @NotNull(message = "validatedByMethodWithDefaultValues message")
    public void setValidatedByMethodWithDefaultValues(Integer validatedByMethodWithDefaultValues) {
        this.validatedByMethodWithDefaultValues = validatedByMethodWithDefaultValues;
    }


    protected void changeValidatedByParameter(@ApiErrorValue(errorCode = "validatedByParameter errorCode", httpStatusCode = 401)
                                              @NotNull(message = "validatedByParameter message")
                                                      String validatedByParameter) {
        this.validatedByParameter = validatedByParameter;
    }

    protected void changeValidatedByParameterWithDefaultValues(@ApiErrorValue
                                                               @NotNull(message = "validatedByParameterWithDefaultValues message")
                                                                       Integer validatedByParameterWithDefaultValues) {
        this.validatedByParameterWithDefaultValues = validatedByParameterWithDefaultValues;
    }

    @Constraint(validatedBy = CustomConstraintValidationAnnotationImpl.class)
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomConstraintValidationAnnotation {

        String message() default "CustomConstraintValidation default message";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }

    private static class CustomConstraintValidationAnnotationImpl
            implements ConstraintValidator<CustomConstraintValidationAnnotation, String> {

        @Override
        public void initialize(CustomConstraintValidationAnnotation constraintAnnotation) {
            // do nothing.
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && !value.isEmpty();
        }

    }

    @ApiErrorValue(errorCode = "customDelegatedValidationAnnotation errorCode", httpStatusCode = 401)
    @NotNull(message = "customDelegatedValidationAnnotation message")
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CustomDelegatedValidationAnnotation { }

    @ApiErrorValue
    @NotNull(message = "customDelegatedValidationAnnotationWithDefaultValues message")
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface CustomDelegatedValidationAnnotationWithDefaultValues { }

}
