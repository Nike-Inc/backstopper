package com.nike.backstopper.validation.constraints.impl;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation of the validation logic for {@link StringConvertsToClassType}. See that annotation's javadocs for more
 * info.
 *
 * @author Nic Munroe
 */
@SuppressWarnings({"WeakerAccess", "ResultOfMethodCallIgnored"})
public class StringConvertsToClassTypeValidator implements ConstraintValidator<StringConvertsToClassType, String> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Class<?> desiredClass;
    private boolean allowCaseInsensitiveEnumMatch;

    @Override
    public void initialize(StringConvertsToClassType constraintAnnotation) {
        this.desiredClass = constraintAnnotation.classType();
        this.allowCaseInsensitiveEnumMatch = constraintAnnotation.allowCaseInsensitiveEnumMatch();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null is always considered valid
        if (value == null)
            return true;

        // Check enums
        if (desiredClass.isEnum())
            return validateAsEnum(value);

        // Check all the primitive types - note that the boxed type and primitive type cannot be contained in a
        //      single isAssignableFrom check
        if (isDesiredClassAssignableToOneOf(Byte.class, byte.class))
            return validateAsByte(value);

        if (isDesiredClassAssignableToOneOf(Short.class, short.class))
            return validateAsShort(value);

        if (isDesiredClassAssignableToOneOf(Integer.class, int.class))
            return validateAsInt(value);

        if (isDesiredClassAssignableToOneOf(Long.class, long.class))
            return validateAsLong(value);

        if (isDesiredClassAssignableToOneOf(Float.class, float.class))
            return validateAsFloat(value);

        if (isDesiredClassAssignableToOneOf(Double.class, double.class))
            return validateAsDouble(value);

        if (isDesiredClassAssignableToOneOf(Boolean.class, boolean.class))
            return validateAsBoolean(value);

        if (isDesiredClassAssignableToOneOf(Character.class, char.class))
            return validateAsChar(value);

        // Strings are strings and always considered valid
        if (isDesiredClassAssignableToOneOf(String.class))
            return true;

        // At this point we don't recognize the class type and must return false, but we should log it as an error
        //      because whoever put the annotation on the field expected us to be able to handle it, so this class
        //      either needs to be expanded to support the desired class type or this constraint should not be used on
        //      this field.
        logger.error(
            "Unhandled class type in StringConvertsToClassTypeValidator. This validator either needs to be expanded to "
            + "support the class type or this constraint should not be used on the field. "
            + "converts_to_class_type_validator_unhandled_class_type=true, class_type={}, field_value={}", desiredClass,
            value);

        return false;
    }

    protected boolean isDesiredClassAssignableToOneOf(Class<?>... allowedClasses) {
        for (Class<?> allowedClass : allowedClasses) {
            if (allowedClass.isAssignableFrom(desiredClass))
                return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    protected boolean validateAsEnum(String value) {
        try {
            Enum.valueOf((Class<Enum>) desiredClass, value);
            // No error, so it can be successfully parsed to this enum type as-is.
            return true;
        }
        catch (Exception ex) {
            // Swallow the exception.
        }

        if (!allowCaseInsensitiveEnumMatch) {
            // Case insensitive matching is not allowed, so at this point we have to return false.
            return false;
        }

        // Case insensitive matching is allowed, so do it.
        Object[] enumValues = desiredClass.getEnumConstants();
        if (enumValues == null)
            return false;

        for (Object enumValue : enumValues) {
            if (enumValue instanceof Enum && ((Enum) enumValue).name().equalsIgnoreCase(value))
                return true;
        }

        // Couldn't convert the given string to the given enum even with a case insensitive check, so return false.
        return false;
    }

    protected boolean validateAsByte(String value) {
        try {
            Byte.parseByte(value);
            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsShort(String value) {
        try {
            Short.parseShort(value);
            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsInt(String value) {
        try {
            Integer.parseInt(value);
            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsLong(String value) {
        try {
            Long.parseLong(value);
            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsFloat(String value) {
        try {
            Float floatValue = Float.parseFloat(value);
            if (floatValue.isInfinite() || floatValue.isNaN())
                return false;

            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsDouble(String value) {
        try {
            Double doubleValue = Double.parseDouble(value);
            if (doubleValue.isInfinite() || doubleValue.isNaN())
                return false;

            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    protected boolean validateAsBoolean(String value) {
        try {
            // We can't use Boolean.parseBoolean(String) because it converts garbage to false. We want to restrict the
            //      value to true or false (ignoring case). Anything else is invalid.

            //noinspection RedundantIfStatement
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
                return true;

            return false;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected boolean validateAsChar(String value) {
        try {
            if (value.length() != 1)
                return false;

            value.charAt(0);
            // No error, so it can be successfully parsed to this primitive type.
            return true;
        }
        catch (Exception ex) {
            // Couldn't parse the given string into this primitive type, so it's not valid.
            return false;
        }
    }
}
