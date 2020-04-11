package com.nike.backstopper.apierror;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * This annotation provides the ability to autoconfigure {@link ProjectApiErrors} with {@link ApiError}s
 * and can be used with already existing {@link ProjectApiErrors} and {@link ApiError}s:
 *
 * <pre>
 * {@code
 *
 * public class SampleModel {
 *
 *     @ApiErrorValue
 *     @NotBlank
 *     public String foo;
 *
 *     @ApiErrorValue(errorCode = "BLANK_BAR", httpStatusCode = 400)
 *     @NotBlank(message = "bar should not be blank")
 *     public String bar;
 *     // -- SNIP --
 * }
 *
 * }
 * </pre>
 * bad request's response will be as follows:
 * <pre>
 * {@code
 *
 * {
 *   "error_id": "c6bbec89-c39a-4164-9155-fd8e1c1a5cbc",
 *   "errors": [
 *     {
 *       "code": "INVALID_VALUE",
 *       "message": "may not be empty",
 *       "metadata": {
 *         "field": "foo"
 *       }
 *     },
 *     {
 *       "code": "BLANK_BAR",
 *       "message": "bar should not be blank",
 *       "metadata": {
 *         "field": "bar"
 *       }
 *     }
 *   ]
 * }
 *
 * }
 * </pre>
 * Supports:
 * 1. default {@code org/hibernate/validator/ValidationMessages.properties} without localization
 * 2. custom {@code ValidationMessages.properties} without localization, has higher priority than default
 * 3. for each element, one {@link ApiErrorValue} with N constraint annotations can be used,
 * including inheritance (annotation over annotation)
 * 4. Spring 4/Spring Boot 1.x/Spring 2.x
 * 5. JDK 7 or later
 * 6. multi module Gradle/Maven projects
 * <p>
 * Requirements:
 * 1. the {@code com.nike} package should be added to scan {@code ComponentScan#basePackages()}
 * 2. {@link ApiErrorValue#errorCode()} value should not be {@code null} or blank
 * 3. {@link ApiErrorValue#httpStatusCode()} value should not be negative
 * 4. {@link ApiErrorValue} used with annotated/inherited JSR 303 constraint annotations
 * or a valid constraint annotations such as Hibernate/custom
 * 5. the constraint annotations should not contains {@code null} or blank {@link String} value
 * for a {@code message} method
 * 6. only one {@link ApiErrorValue} will be applied for one element,
 * in case of collision only the first one and excluding the inheriting one
 * <p>
 * Notice:
 * If some {@code message} value is not unique in some model,
 * it is recommended to use the same {@link ApiErrorValue#errorCode()} and {@link ApiErrorValue#httpStatusCode()}
 * values in this model:
 * <pre>
 * {@code
 *
 * public class SampleModel {
 *
 *     @ApiErrorValue(errorCode = "BLANK_VALUE", httpStatusCode = 400)
 *     @NotBlank(message = "value should not be blank")
 *     public final String foo;
 *
 *     @ApiErrorValue(errorCode = "BLANK_VALUE", httpStatusCode = 400)
 *     @NotBlank(message = "value should not be blank")
 *     public final String bar;
 *     // -- SNIP --
 * }
 *
 * }
 * </pre>
 * and avoid such cases with the same {@code message} value with different
 * {@link ApiErrorValue#errorCode()} and {@link ApiErrorValue#httpStatusCode()} values in the model:
 * <pre>
 * {@code
 *
 * public class SampleModel {
 *
 *     @ApiErrorValue(errorCode = "EMPTY_VALUE", httpStatusCode = 402)
 *     @NotBlank(message = "value should not be blank")
 *     public final String foo;
 *
 *     @ApiErrorValue(errorCode = "BLANK_VALUE", httpStatusCode = 400)
 *     @NotBlank(message = "value should not be blank")
 *     public final String bar;
 *     // -- SNIP --
 * }
 *
 * }
 * if the validation failed, an {@link ApiError} will be found by {@code message} value on the first match.
 * <p>
 * Implementation of auto-configuration at runtime:
 * 1. if {@link ApiErrorValue} is used with already existing {@link ProjectApiErrors} with {@link ApiError}s
 * in this case {@link ProjectApiErrors} are redefined by merging
 * auto-configured {@link ApiError}s based on {@link ApiErrorValue}
 * to existing {@code ProjectApiErrors#getProjectSpecificApiErrors()}
 * 2. otherwise {@link SampleProjectApiErrorsBase} will be implemented defining two methods:
 * 2.1. {@code SampleProjectApiErrorsBase#getProjectSpecificApiErrors()} using auto-configured
 * {@link ApiError}s based on {@link ApiErrorValue},
 * 2.2. {@code SampleProjectApiErrorsBase#getProjectSpecificErrorCodeRange()} using
 * {@link ProjectSpecificErrorCodeRange#ALLOW_ALL_ERROR_CODES}
 *
 * @author Andrey Tsarenko
 */
@Retention(RetentionPolicy.CLASS)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
public @interface ApiErrorValue {

    /**
     * The business/project error code, will be converted to {@link ApiError#getErrorCode()}.
     * A value should not be {@code null} or blank.
     *
     * @return the error code.
     * @see ApiError#getErrorCode()
     */
    String errorCode() default "INVALID_VALUE";

    /**
     * The HTTP status code, will be converted to {@link ApiError#getHttpStatusCode()}.
     * A value should not be negative.
     *
     * @return the error code.
     * @see ApiError#getHttpStatusCode()
     */
    int httpStatusCode() default 400;

}
