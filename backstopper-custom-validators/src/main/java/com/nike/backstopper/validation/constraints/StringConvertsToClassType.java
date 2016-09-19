package com.nike.backstopper.validation.constraints;

import com.nike.backstopper.validation.constraints.impl.StringConvertsToClassTypeValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that the annotated element (of type String) can be converted to the given {@link #classType()}. The
 * {@code classType} can be any of the following:
 * <ul>
 *     <li>
 *         Any boxed primitive class type (e.g. {@code Integer.class}) - Validated via
 *         {@code Primitive.parsePrimitive(String)} (e.g. {@link Integer#parseInt(String)})
 *     </li>
 *     <li>
 *         Any raw primitive class type (e.g. {@code int.class}) - Also validated via
 *         {@code Primitive.parsePrimitive(String)} (e.g. {@link Integer#parseInt(String)})
 *     </li>
 *     <li>
 *         String.class - a String can always be converted to a String, so this validator will always return true in
 *         this case
 *     </li>
 *     <li>
 *         Any enum class type - validation is done by comparing the string value to {@link Enum#name()}. The value of
 *         {@link #allowCaseInsensitiveEnumMatch()} determines if the validation is done in a case sensitive or case
 *         insensitive manner.
 *         <p>
 *         <strong>
 *             IMPORTANT NOTE: If you set {@link #allowCaseInsensitiveEnumMatch()} to true, then you must support this
 *             case insensitivity when deserializing the string to the desired Enum class. e.g. If you're using Jackson
 *             then you can make the enum case insensitive during Jackson deserialization via a
 *             {@code com.fasterxml.jackson.annotation.JsonCreator} annotated method in the enum. Here's an example of
 *             how to set up an enum that would pass this case-insensitivity requirement:
 *         </strong>
 *             <pre>
 *                  public enum RgbColors {
 *                      RED, GREEN, BLUE;
 *
 *                      &#64;JsonCreator
 *                      public static RgbColors toRgbColor(String colorString) {
 *                          for (RgbColors color : values()) {
 *                              if (color.name().equalsIgnoreCase(colorString))
 *                                  return color;
 *                          }
 *                          throw new IllegalArgumentException("Cannot convert the string: \"" + colorString
 *                                                             + "\" to a valid RgbColors enum value.");
 *                      }
 *                  }
 *             </pre>
 *     </li>
 * </ul>
 *
 * <p>{@code null} is always considered valid - if you need to enforce non-null then you should place an additional
 * {@link javax.validation.constraints.NotNull} constraint on the field as well.
 *
 * <p>Note that Floats and Doubles will fail validation if the number parses to {@link Float#isInfinite()},
 * {@link Float#isNaN()}, {@link Double#isInfinite()}, or {@link Double#isNaN()}.
 *
 * <p><b>NOTE: THIS ANNOTATION MUST BE PLACED ON A STRING ELEMENT ONLY</b>
 *
 * @author Nic Munroe
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = StringConvertsToClassTypeValidator.class)
public @interface StringConvertsToClassType {
    String message() default "{StringConvertsToClassType.message}";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default {};

    Class<?> classType();
    boolean allowCaseInsensitiveEnumMatch() default false;
}
