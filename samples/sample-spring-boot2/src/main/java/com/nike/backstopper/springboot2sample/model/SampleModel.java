package com.nike.backstopper.springboot2sample.model;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.springboot2sample.error.SampleProjectApiError;
import com.nike.backstopper.springboot2sample.error.SampleProjectApiErrorsImpl;
import com.nike.backstopper.validation.constraints.StringConvertsToClassType;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Simple model class showing the JSR 303 Bean Validation integration in Backstopper. Each message for a JSR 303
 * annotation must match an {@link ApiError#getName()} from one of the errors returned by this project's
 * {@link SampleProjectApiErrorsImpl#getProjectApiErrors()}. In this case that means you can use any of the enum
 * names from {@link SampleCoreApiError} or {@link SampleProjectApiError}.
 *
 * <p>If you have a typo or forget to add a message that matches an error name then the {@code VerifyJsr303ContractTest}
 * unit test will catch your error and the project will fail to build - the test will give you info on exactly which
 * classes, fields, and annotations don't conform to the necessary convention.
 */
public class SampleModel {
    @NotBlank(message = "FOO_STRING_CANNOT_BE_BLANK")
    public final String foo;

    @Range(message = "INVALID_RANGE_VALUE", min = 0, max = 42)
    public final String range_0_to_42;

    @NotNull(message = "RGB_COLOR_CANNOT_BE_NULL")
    @StringConvertsToClassType(
        message = "NOT_RGB_COLOR_ENUM", classType = RgbColor.class, allowCaseInsensitiveEnumMatch = true
    )
    public final String rgb_color;

    public final Boolean throw_manual_error;

    @SuppressWarnings("unused")
    // Intentionally protected - here for deserialization support.
    protected SampleModel() {
        this(null, null, null, null);
    }

    public SampleModel(String foo, String range_0_to_42, String rgb_color, Boolean throw_manual_error) {
        this.foo = foo;
        this.range_0_to_42 = range_0_to_42;
        this.rgb_color = rgb_color;
        this.throw_manual_error = throw_manual_error;
    }
}
