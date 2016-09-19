package com.nike.backstopper.jersey2sample.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * An enum used by {@link SampleModel} for showing how
 * {@link com.nike.backstopper.validation.constraints.StringConvertsToClassType} can work with enums. Note
 * the {@link #toRgbColor(String)} annotated with {@link JsonCreator}, which allows callers to pass in lower or
 * mixed case versions of the enum values and still have them automatically deserialized to the correct enum.
 * This special {@link JsonCreator} method is only necessary if you want to support case-insensitive enum validation
 * when deserializing.
 */
public enum RgbColor {
    RED, GREEN, BLUE;

    @JsonCreator
    @SuppressWarnings("unused")
    public static RgbColor toRgbColor(String colorString) {
        for (RgbColor color : values()) {
            if (color.name().equalsIgnoreCase(colorString))
                return color;
        }
        throw new IllegalArgumentException(
            "Cannot convert the string: \"" + colorString + "\" to a valid RgbColor enum value."
        );
    }
}
