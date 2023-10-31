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
 *     @Pattern(regexp = "bar", message = "should match {bar}")
 *     public String bar;
 *     // -- SNIP --
 * }
 *
 * }
 * </pre>
 * the response to an empty request will be as follows:
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
 * the response to the request with an empty {@code foo} and an invalid {@code bar} will be as follows:
 * <pre>
 * {@code
 *
 * {
 *   "error_id": "d3d5d6d9-af31-48bf-96a7-7d91cff29cd5",
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
 *       "message": "should match {bar}",
 *       "metadata": {
 *         "field": "bar"
 *       }
 *     }
 *   ]
 * }
 *
 * }
 * </pre>
 * <p>
 * Supports:
 * 1. default {@code org/hibernate/validator/ValidationMessages.properties} without localization
 * 2. custom {@code ValidationMessages.properties} without localization, has higher priority than default
 * 3. for each element, one {@link ApiErrorValue} with N constraint annotations can be used,
 * including inheritance (annotation over annotation)
 * 4. Spring 4.x/5.x, Spring Boot 1.x/2.x
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
 * in case of collision only the first one and excluding the inheriting one (can also be seen in compilation logs)
 * <p>
 * If the requirements are not met then the compilation will be interrupted with an error and the corresponding message.
 * <p>
 * Notices:
 * If some {@code message} value is not unique in some model,
 * it is recommended to use the same {@link ApiErrorValue#errorCode()} and {@link ApiErrorValue#httpStatusCode()}
 * values in this model:
 * <pre>
 * {@code
 *
 * public class SampleModel {
 *
 *     @ApiErrorValue(errorCode = "SOME_ERROR_CODE", httpStatusCode = 400)
 *     @NotBlank(message = "value should not be blank")
 *     public final String foo;
 *
 *     @ApiErrorValue(errorCode = "SOME_ERROR_CODE", httpStatusCode = 400)
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
 * if the validation failed, an {@link ApiError} will be found by {@code message} value on the first match
 * and determine {@link ApiError#getErrorCode()} and {@link ApiError#getHttpStatusCode()} by the first found,
 * otherwise you can configure {@link ProjectApiErrors#getStatusCodePriorityOrder()}.
 * <p>
 * Testing:
 * 1. if you use {@link ApiErrorValue} with existing {@link ProjectApiErrors} and {@link ApiError}s,
 * you do not need to change your implementation based on the {@code backstopper-reusable-tests},
 * since {@link ApiErrorValue}-based {@link ProjectApiErrors} and {@link ApiError}s is already checked at compile time.
 * 2. if you use only {@link ApiErrorValue} you do not need to use the {@code backstopper-reusable-tests},
 * as mentioned above this is already checked at compile time.
 * <p>
 * Details of auto-configuration implementation at runtime:
 * 1. if {@link ApiErrorValue} is used with already existing {@link ProjectApiErrors} with {@link ApiError}s
 * in this case {@link ProjectApiErrors} are redefined by merging
 * auto-configured {@link ApiError}s based on {@link ApiErrorValue}
 * to existing {@code ProjectApiErrors#getProjectSpecificApiErrors()}
 * 2. otherwise {@link SampleProjectApiErrorsBase} will be implemented defining two methods:
 * 2.1. {@code SampleProjectApiErrorsBase#getProjectSpecificApiErrors()} using auto-configured
 * {@link ApiError}s based on {@link ApiErrorValue},
 * 2.2. {@code SampleProjectApiErrorsBase#getProjectSpecificErrorCodeRange()} using
 * {@link ProjectSpecificErrorCodeRange#ALLOW_ALL_ERROR_CODES},
 * if the default implementation {@link SampleProjectApiErrorsBase} is not suitable for you,
 * you can provide custom implementation of {@link ProjectApiErrors}
 * with empty {@code ProjectApiErrors#getProjectSpecificApiErrors()}.
 *
 * @author Andrey Tsarenko
 */
@Retention(RetentionPolicy.RUNTIME)
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
