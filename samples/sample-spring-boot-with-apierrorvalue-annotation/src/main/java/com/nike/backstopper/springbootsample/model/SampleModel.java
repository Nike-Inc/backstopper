package com.nike.backstopper.springbootsample.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

/**
 * Simple model class showing JSR 303 Bean Validation integration in Backstopper using {@link ApiErrorValue},
 * that provides the ability to autoconfigure {@link ProjectApiErrors} with {@link ApiError}s.
 * Can also be used with already existing {@link ProjectApiErrors} and {@link ApiError}s.
 *
 * @author Andrey Tsarenko
 */
public class SampleModel {

    /**
     * Shows {@link ApiErrorValue} with default value:
     * {@link ApiErrorValue#errorCode()}
     * {@link ApiErrorValue#httpStatusCode()}
     * and default value of {@link NotBlank#message()} is mapped to
     * {@code org/hibernate/validator/ValidationMessages.properties},
     * can also be used with custom {@code ValidationMessages.properties}.
     */
    @ApiErrorValue
    @NotBlank
    public final String foo;

    /**
     * Shows customized {@link ApiErrorValue#errorCode()}, {@link NotBlank#message()} and {@link Pattern#message()}.
     */
    @ApiErrorValue(errorCode = "INVALID_BAR_VALUE", httpStatusCode = 400)
    @NotBlank(message = "bar should not be blank")
    @Pattern(regexp = "bar", message = "should match {bar}")
    public final String bar;

    public SampleModel(@JsonProperty("foo") String foo,
                       @JsonProperty("bar") String bar) {
        this.foo = foo;
        this.bar = bar;
    }

}
