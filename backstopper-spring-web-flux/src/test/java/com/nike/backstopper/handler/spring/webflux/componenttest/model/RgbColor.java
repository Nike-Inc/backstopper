package com.nike.backstopper.handler.spring.webflux.componenttest.model;

import com.fasterxml.jackson.annotation.JsonCreator;

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
