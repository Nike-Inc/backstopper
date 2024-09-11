package com.nike.backstopper.handler.spring.webflux.componenttest.model;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;

import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
