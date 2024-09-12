package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.ApiError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_BAD_REQUEST;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_CONFLICT;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_FORBIDDEN;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_METHOD_NOT_ALLOWED;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_NOT_ACCEPTABLE;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_NOT_FOUND;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_SERVICE_UNAVAILABLE;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_TOO_MANY_REQUESTS;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_UNAUTHORIZED;
import static com.nike.backstopper.apierror.ApiErrorConstants.HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE;

/**
 * Class representing the available {@link ApiError}s for a project and related methods for how they should be treated.
 * Normally a concrete instance for a project simply needs to implement the abstract {@link #getCoreApiErrors()},
 * {@link #getProjectSpecificApiErrors()}, {@link #getProjectSpecificErrorCodeRange()}, and {@code get[Special]Error()}
 * methods and the rest is handled for you (see the javadocs for those methods for info on how to implement them). If
 * you need to override other behavior you can however - most notably {@link #getStatusCodePriorityOrder()} (the
 * defaults should be sufficient for most cases but you can override it if necessary).
 *
 * <p>If you need some hints on how to implement the abstract "special error" methods, see
 * {@link com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase} and the
 * {@link com.nike.backstopper.apierror.sample.SampleCoreApiError}s that are used, and copy/paste/modify for your
 * purposes.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class ProjectApiErrors {

    // ====================== ABSTRACT METHODS - see javadocs for implementation details ======================
    /**
     * @return The list of {@link ApiError}s that are common and shared among multiple projects in your organization. In
     *          particular, these {@link ApiError}s will not be subject to range checking (see
     *          {@link #verifyErrorsAreInRange(List, List)}). This list should <b>NOT</b> include any
     *          {@link #getProjectSpecificApiErrors()}.
     *
     *          <p>These core errors will be added automatically to the public-facing {@link #getProjectApiErrors()}
     *          along with the project-specific {@link #getProjectSpecificApiErrors()}.
     *
     *          <p>You can safely return null for this if your project only uses {@link #getProjectSpecificApiErrors()}.
     */
    protected abstract List<ApiError> getCoreApiErrors();

    /**
     * @return The list of {@link ApiError}s specific just to this project. This list should <b>NOT</b> include any
     *          {@link #getCoreApiErrors()} directly although "core error wrappers" are allowed where the
     *          {@link ApiError#getErrorCode()}, {@link ApiError#getMessage()}, and {@link ApiError#getHttpStatusCode()}
     *          exactly match a core error but {@link ApiError#getName()} and (optionally)
     *          {@link ApiError#getMetadata()} are different. Core error wrappers are useful where you want the same
     *          base error contract returned to the caller but you want a different name to show up in the logs to
     *          identify more specifically what went wrong. Any project-specific errors that are not core error wrappers
     *          are subject to range checking and must have a {@link ApiError#getErrorCode()} that falls into the range
     *          specified by {@link #getProjectSpecificErrorCodeRange()} (see
     *          {@link #verifyErrorsAreInRange(List, List)} for validation details).
     *
     *          <p>These project-specific errors will be added automatically to the public-facing
     *          {@link #getProjectApiErrors()} along with the core {@link #getCoreApiErrors()}.
     *
     *          <p>You can safely return null for this if your project only uses {@link #getCoreApiErrors()}.
     */
    protected abstract List<ApiError> getProjectSpecificApiErrors();

    /**
     * @return The {@link ProjectSpecificErrorCodeRange} for this project. This is used to verify that
     *          {@link #getProjectSpecificApiErrors()} doesn't include any error codes outside the range you have
     *          reserved for your project. This can return null, but <b>only</b> if this project uses 100%
     *          {@link #getCoreApiErrors()} and core error wrappers.
     */
    protected abstract ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange();

    /**
     * @return The special {@link ApiError} that should be associated with a generic "something unexpected went wrong"
     *          service error - usually you'd want this to map to a 500 HTTP status code with an appropriate generic
     *          message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getGenericServiceError();

    /**
     * @return The special {@link ApiError} that should be associated with "an outside dependency returned an
     *          unrecoverable error" type generic errors - usually you'd want this to map to a plain 500 HTTP status
     *          code with an appropriate generic "something unexpected went wrong" type of message, but maybe with a
     *          distinctive {@link ApiError#getName()} like {@code OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR}
     *          so that it's obvious what the cause was when you look in the logs. This must be contained in
     *          {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getOusideDependencyReturnedAnUnrecoverableErrorApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "validation error occurred that was not due
     *          to data received from the caller" type of error - usually you'd want this to map to a plain 500 HTTP
     *          status code with an appropriate generic "something unexpected went wrong" type of message, but maybe
     *          with a distinctive {@link ApiError#getName()} like {@code SERVERSIDE_VALIDATION_ERROR} so that it's
     *          obvious what the cause was when you look in the logs. This must be contained in
     *          {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getServersideValidationApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "temporary service problem" - usually you'd
     *          want this to map to a 503 HTTP status code with an appropriate generic "try again later" type of
     *          message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getTemporaryServiceProblemApiError();

    /**
     * @return The special {@link ApiError} that should be associated with "an outside dependency returned a temporary
     *          error" type of error - usually you'd want this to map to a 503 HTTP status code with an appropriate
     *          generic "something unexpected went wrong" type of message, but maybe with a distinctive
     *          {@link ApiError#getName()} like {@code OUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERROR} so that it's
     *          obvious what the cause was when you look in the logs. This must be contained in
     *          {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getOutsideDependencyReturnedTemporaryErrorApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a generic "you sent a bad request" type of
     *          error - usually you'd want this to map to a 400 HTTP status code with an appropriate generic "invalid
     *          request" type of message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getGenericBadRequestApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "you didn't send required content" type of
     *          error - usually you'd want this to map to a 400 HTTP status code with an appropriate "missing expected
     *          content" message, probably with a distinctive {@link ApiError#getName()} so that it's obvious what the
     *          cause was when you look in the logs. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getMissingExpectedContentApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "content could not be converted to the
     *          correct type" type of error (e.g. the caller sent a string value that could not be parsed to the
     *          required integer type) - usually you'd want this to map to a 400 HTTP status code with an appropriate
     *          "type conversion error" message, probably with a distinctive {@link ApiError#getName()} so that it's
     *          obvious what the cause was when you look in the logs. This must be contained in
     *          {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getTypeConversionApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "unable to deserialize data" type of error
     *          (e.g. the caller sent a payload that could not be deserialized to the necessary type and the error
     *          didn't fall into a more specific category) - usually you'd want this to map to a 400 HTTP status code
     *          with an appropriate "malformed request" message, probably with a distinctive {@link ApiError#getName()}
     *          so that it's obvious what the cause was when you look in the logs. This must be contained in
     *          {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getMalformedRequestApiError();

    /**
     * @return The special {@link ApiError} that should be associated with an "unauthorized request" error - usually
     *          you'd want this to map to a 401 HTTP status code with an appropriate "unauthorized request" message.
     *          This must be contained in {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getUnauthorizedApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "forbidden request" error - usually you'd
     *          want this to map to a 403 HTTP status code with an appropriate "forbidden request" message. This must be
     *          contained in {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getForbiddenApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "requested resource not found" error -
     *          usually you'd want this to map to a 404 HTTP status code with an appropriate "requested resource not
     *          found" message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getNotFoundApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "HTTP method not allowed" error. usually
     *          you'd want this to map to a 405 HTTP status code with an appropriate "HTTP method not allowed for this
     *          resource" message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getMethodNotAllowedApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "no acceptable representation" error.
     *          usually you'd want this to map to a 406 HTTP status code with an appropriate "no acceptable
     *          representation for this resource" message. This must be contained in {@link #getCoreApiErrors()} or
     *          {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getNoAcceptableRepresentationApiError();

    /**
     * @return The special {@link ApiError} that should be associated with an "unsupported media type" error. usually
     *          you'd want this to map to a 415 HTTP status code with an appropriate "unsupported media type" message.
     *          This must be contained in {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getUnsupportedMediaTypeApiError();

    /**
     * @return The special {@link ApiError} that should be associated with a "too many requests / rate limited" type of
     *          error - usually you'd want this to map to a 429 HTTP status code with appropriate message. This must be
     *          contained in {@link #getCoreApiErrors()} or {@link #getProjectSpecificApiErrors()}.
     */
    public abstract ApiError getTooManyRequestsApiError();

    // ====================== NON-ABSTRACT CLASS DEFINITION ======================
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * This list represents the default priority order of status codes. Since we might gather multiple {@link ApiError}s
     * for a given request but we have to return only one HTTP status code in the response, this list tells you which
     * one should be used (earlier in the list means higher priority).
     * {@link #determineHighestPriorityHttpStatusCode(Collection)} provides a helper method for taking a list of
     * {@link ApiError}s and returning the appropriate HTTP status code based on a priority list like this one.
     */
    public static final List<Integer> DEFAULT_STATUS_CODE_PRIORITY_ORDER = Arrays.asList(
        HTTP_STATUS_CODE_FORBIDDEN, HTTP_STATUS_CODE_UNAUTHORIZED, HTTP_STATUS_CODE_SERVICE_UNAVAILABLE,
        HTTP_STATUS_CODE_TOO_MANY_REQUESTS, HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR, HTTP_STATUS_CODE_METHOD_NOT_ALLOWED,
        HTTP_STATUS_CODE_NOT_ACCEPTABLE, HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE, HTTP_STATUS_CODE_NOT_FOUND,
        HTTP_STATUS_CODE_CONFLICT,
        HTTP_STATUS_CODE_BAD_REQUEST);

    /**
     * Private cache of the full {@link ApiError} list returned by {@link #getProjectApiErrors()} so that we don't have
     * to calculate it every time it's asked for.
     */
    private List<ApiError> projectApiErrorsCache;

    /**
     * Default constructor. Calls {@link #getProjectApiErrors()} so that we fail-fast if there are any problems with the
     * way a given project's concrete implementation is defined.
     */
    public ProjectApiErrors() {
        getProjectApiErrors();
    }

    /**
     * @return The full list of {@link ApiError}s available for this project. Includes all of
     *          {@link #getCoreApiErrors()} combined with {@link #getProjectSpecificApiErrors()}.
     */
    public List<ApiError> getProjectApiErrors() {
        if (projectApiErrorsCache == null) {
            // No cached list yet. Create one.
            List<ApiError> projectApiErrors = new ArrayList<>();

            // Add all the core errors (if we have any).
            List<ApiError> coreApiErrors = getCoreApiErrors();
            if (coreApiErrors != null) {
                projectApiErrors.addAll(coreApiErrors);
            }

            // Add all the project-specific errors (if we have any).
            List<ApiError> projectSpecificApiErrors = getProjectSpecificApiErrors();
            if (projectSpecificApiErrors != null) {
                // We have some project-specific errors. Verify that they are within the allowed range for this project.
                verifyErrorsAreInRange(projectSpecificApiErrors, coreApiErrors);
                // They are all valid - add them to the final list of project API errors.
                projectApiErrors.addAll(projectSpecificApiErrors);
            }

            // Verify that all the special errors are contained in the final full list of project API errors.
            verifySpecialErrorsAreContainedInApiErrorList(projectApiErrors);

            // Cache the result.
            projectApiErrorsCache = projectApiErrors;
        }
        return projectApiErrorsCache;
    }

    /**
     * Shortcut for calling {@link #convertToApiError(String, ApiError)} and passing null as the fallback.
     *
     * @return The {@link ApiError} for this project associated with the given name, or null if no such ApiError exists.
     *          <p>NOTE: This method uses {@link ApiError#getName()} to match against the given name argument, and uses
     *          {@link #getProjectApiErrors()} as the list of {@link ApiError}s to search through.
     */
    public ApiError convertToApiError(String name) {
        return convertToApiError(name, null);
    }

    /**
     * @return The {@link ApiError} for this project associated with the given name, or
     *          {@code fallbackDefaultIfUnconvertible} if no such ApiError exists.
     *          <p>NOTE: This method uses {@link ApiError#getName()} to match against the given name argument, and uses
     *          {@link #getProjectApiErrors()} as the list of {@link ApiError}s to search through.
     */
    public ApiError convertToApiError(String name, ApiError fallbackDefaultIfUnconvertible) {
        if (name == null) {
            return fallbackDefaultIfUnconvertible;
        }

        for (ApiError apiError : getProjectApiErrors()) {
            if (name.equals(apiError.getName())) {
                return apiError;
            }
        }
        return fallbackDefaultIfUnconvertible;
    }

    /**
     * @return A list representing the priority order for HTTP status codes for this project. Since we might gather
     *          multiple {@link ApiError}s for a given request but we have to return only one HTTP status code in the
     *          response, this list tells you which one should be used (earlier in the list means higher priority).
     *          {@link #determineHighestPriorityHttpStatusCode(Collection)} provides a helper method for taking a list
     *          of {@link ApiError}s and returning the appropriate HTTP status code based on this priority list. This
     *          method defaults to returning the {@link #DEFAULT_STATUS_CODE_PRIORITY_ORDER} list. Override this method
     *          in concrete subclasses if your project needs a different order.
     */
    public List<Integer> getStatusCodePriorityOrder() {
        return DEFAULT_STATUS_CODE_PRIORITY_ORDER;
    }

    /**
     * @return The HTTP status code contained in the given collection of {@link ApiError}s that comes first in
     *          {@link #getStatusCodePriorityOrder()}, or null if the given collection is null or empty. It is also
     *          technically possible to return null if none of the errors you passed in have a status code contained in
     *          the priority list, but this should be considered a bug that needs to be fixed (you should have a unit
     *          test for your project that prevents this from happening). In any case you should defensively code for
     *          the possibility of null being returned and have a strategy for picking a winner.
     */
    public Integer determineHighestPriorityHttpStatusCode(Collection<ApiError> apiErrors) {
        if (apiErrors == null || apiErrors.isEmpty()) {
            return null;
        }

        // If we only have one error we don't need to go any further
        if (apiErrors.size() == 1) {
            return apiErrors.iterator().next().getHttpStatusCode();
        }

        // Convert the list of errors to the set of http status codes they represent
        Set<Integer> validStatusCodePossibilities = new HashSet<>();
        for (ApiError ae : apiErrors) {
            validStatusCodePossibilities.add(ae.getHttpStatusCode());
        }

        // If we only have one HTTP status code we can return it now (no possibility of conflict)
        if (validStatusCodePossibilities.size() == 1) {
            return validStatusCodePossibilities.iterator().next();
        }

        // Run through the priority order. The first one we find that is also contained in validStatusCodePossibilities
        //      is the one that should be used.
        for (Integer statusCode : getStatusCodePriorityOrder()) {
            if (validStatusCodePossibilities.contains(statusCode)) {
                return statusCode;
            }
        }

        // Shouldn't get here in a properly setup project. Log an error and return null.
        logger.error(
            "None of the HTTP status codes in the ApiErrors passed to determineHighestPriorityHttpStatusCode() were"
            + " found in the getStatusCodePriorityOrder() list. Offending set of http status codes (these should be "
            + "added to the getStatusCodePriorityOrder() list for this project): {}", validStatusCodePossibilities
        );

        return null;
    }

    /**
     * @return The {@code fullList} passed in after it has been filtered to only contain {@link ApiError}s with the
     *          given {@code filterHttpStatusCode}. This will return an empty list if either argument is null (or if
     *          there are no errors in the list that match the filter). It will never return null. NOTE: This method
     *          uses a for loop to run through the given {@code fullList}, so if the collection you pass in does not
     *          have a defined order (e.g. a Set), then the returned list's order is undefined. If the collection you
     *          pass in has a defined order, then the output list will be in the same order minus any items that are
     *          removed.
     */
    public List<ApiError> getSublistContainingOnlyHttpStatusCode(Collection<ApiError> fullList,
                                                                 Integer filterHttpStatusCode) {
        if (fullList == null || filterHttpStatusCode == null) {
            return Collections.emptyList();
        }

        List<ApiError> filteredList = new ArrayList<>();
        for (ApiError ae : fullList) {
            if (ae.getHttpStatusCode() == filterHttpStatusCode) {
                filteredList.add(ae);
            }
        }

        return filteredList;
    }

    /**
     * Verifies that all the special errors are not null and can be found in the given full list of project API errors
     * (both core and project-specific errors). An {@link IllegalStateException} will be thrown if any of the special
     * errors are null or cannot be found in the full project API errors list. The special errors are:
     * <ul>
     * <li>{@link #getGenericServiceError()}</li>
     * <li>{@link #getOusideDependencyReturnedAnUnrecoverableErrorApiError()}</li>
     * <li>{@link #getServersideValidationApiError()}</li>
     * <li>{@link #getTemporaryServiceProblemApiError()}</li>
     * <li>{@link #getOutsideDependencyReturnedTemporaryErrorApiError()}</li>
     * <li>{@link #getGenericBadRequestApiError()}</li>
     * <li>{@link #getMissingExpectedContentApiError()}</li>
     * <li>{@link #getTypeConversionApiError()}</li>
     * <li>{@link #getMalformedRequestApiError()}</li>
     * <li>{@link #getUnauthorizedApiError()}</li>
     * <li>{@link #getForbiddenApiError()}</li>
     * <li>{@link #getNotFoundApiError()}</li>
     * <li>{@link #getMethodNotAllowedApiError()}</li>
     * <li>{@link #getNoAcceptableRepresentationApiError()}</li>
     * <li>{@link #getUnsupportedMediaTypeApiError()}</li>
     * <li>{@link #getTooManyRequestsApiError()}</li>
     * </ul>
     *
     * @param projectApiErrors The full list of project API errors including both core errors and project-specific
     *                         errors.
     */
    protected void verifySpecialErrorsAreContainedInApiErrorList(List<ApiError> projectApiErrors) {
        verifySpecialErrorIsContainedInApiErrorList(
            getGenericServiceError(), projectApiErrors, "getGenericServiceError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getOusideDependencyReturnedAnUnrecoverableErrorApiError(), projectApiErrors,
            "getOusideDependencyReturnedAnUnrecoverableErrorApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getServersideValidationApiError(), projectApiErrors, "getServersideValidationApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getTemporaryServiceProblemApiError(), projectApiErrors,
                                                    "getTemporaryServiceProblemApiError");
        verifySpecialErrorIsContainedInApiErrorList(
            getOutsideDependencyReturnedTemporaryErrorApiError(), projectApiErrors,
            "getOutsideDependencyReturnedTemporaryErrorApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getGenericBadRequestApiError(), projectApiErrors, "getGenericBadRequestApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getMissingExpectedContentApiError(), projectApiErrors, "getMissingExpectedContentApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getTypeConversionApiError(), projectApiErrors, "getTypeConversionApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getMalformedRequestApiError(), projectApiErrors, "getMalformedRequestApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getUnauthorizedApiError(), projectApiErrors, "getUnauthorizedApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(getForbiddenApiError(), projectApiErrors, "getForbiddenApiError");
        verifySpecialErrorIsContainedInApiErrorList(getNotFoundApiError(), projectApiErrors, "getNotFoundApiError");
        verifySpecialErrorIsContainedInApiErrorList(
            getMethodNotAllowedApiError(), projectApiErrors, "getMethodNotAllowedApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getNoAcceptableRepresentationApiError(), projectApiErrors, "getNoAcceptableRepresentationApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getUnsupportedMediaTypeApiError(), projectApiErrors, "getUnsupportedMediaTypeApiError"
        );
        verifySpecialErrorIsContainedInApiErrorList(
            getTooManyRequestsApiError(), projectApiErrors, "getTooManyRequestsApiError"
        );
    }

    /**
     * Throws an {@link IllegalStateException} if the given special error is null or if it can't be found in the given
     * full list of project API errors.
     *
     * @param specialError           The special error to check.
     * @param projectApiErrors       The full list of project API errors (both core and project-specific).
     * @param specialErrorMethodName The method name that returns the given special error. Used in the exception message
     *                               if an {@link IllegalStateException} needs to be thrown so you know which method to
     *                               fix.
     */
    protected void verifySpecialErrorIsContainedInApiErrorList(ApiError specialError, List<ApiError> projectApiErrors,
                                                               String specialErrorMethodName) {
        if (specialError == null) {
            throw new IllegalStateException(
                "Special error method " + specialErrorMethodName + "() cannot return null. Class with illegal state: "
                + this.getClass().getName()
            );
        }

        if (!projectApiErrors.contains(specialError)) {
            throw new IllegalStateException(
                "Special error method " + specialErrorMethodName + "() returned an ApiError (" + specialError.getName()
                + ") that was not found in the full getProjectApiErrors() list. This is not allowed - all special "
                + "errors must be contained in the getProjectApiErrors() list. Class with illegal state: "
                + this.getClass().getName()
            );
        }
    }

    /**
     * Compares the list of project-specific errors against {@link #getProjectSpecificErrorCodeRange()} to verify that
     * all errors fall within this project's valid range. If there is an error that falls outside the valid range then
     * an {@link java.lang.IllegalStateException} will be thrown.
     *
     * <p>The given list of core errors is used to determine if any of the project-specific errors are "wrappers" around
     * a core error, which is the only allowed case where a project-specific error can have an error code outside the
     * allowed range.
     *
     * <p>An {@link java.lang.IllegalStateException} will also be thrown if {@link #getProjectSpecificErrorCodeRange()}
     * is null but you have any project-specific errors that are not core error wrappers (the
     * {@link #getProjectSpecificErrorCodeRange()} is allowed to be null only if you have 100% core errors or core error
     * wrappers).
     */
    protected void verifyErrorsAreInRange(List<ApiError> projectSpecificErrors, List<ApiError> coreErrors) {
        ProjectSpecificErrorCodeRange validRange = getProjectSpecificErrorCodeRange();

        for (ApiError projectError : projectSpecificErrors) {
            // Ignore wrappers around core errors
            boolean isCoreError = isWrapperAroundCoreError(projectError, coreErrors);

            if (!isCoreError) {
                // It's not a wrapper around a core error.

                // If validRange is null at this point then that constitutes an error since
                //      getProjectSpecificErrorCodeRange() is only allowed to be null if the project is
                //      100% core errors.
                if (validRange == null) {
                    throw new IllegalStateException(
                        "The ProjectSpecificErrorCodeRange for this project is null, but there is an ApiError that is "
                        + "not a core error. This project must have a ProjectSpecificErrorCodeRange that covers all "
                        + "non-core errors. Offending ApiError: " + projectError.getName()
                    );
                }

                // Check to make sure the project-specific error falls into the project's error range.
                if (!validRange.isInRange(projectError)) {
                    throw new IllegalStateException(
                        "Found ApiError for this project with an error code that does not fall within the valid range "
                        + "specified by " + validRange.getName() + ". ApiError: " + projectError.getName()
                        + ". Error code value: " + projectError.getErrorCode()
                    );
                }
            }
        }
    }

    /**
     * @return true if the given {@link ApiError} is a "wrapper" around one of the given core errors, false otherwise. A
     *          "wrapper" means it shares the same client-facing properties; its {@link ApiError#getErrorCode()},
     *          {@link ApiError#getMessage()}, and {@link ApiError#getHttpStatusCode()} must exactly match one of the
     *          core error instances. The {@link ApiError#getName()} and {@link ApiError#getMetadata()} do not have to
     *          match.
     */
    protected boolean isWrapperAroundCoreError(ApiError projectError, List<ApiError> coreErrors) {
        if (projectError == null) {
            return false;
        }

        for (ApiError coreApiError : coreErrors) {
            boolean errorCodeMatches = Objects.equals(projectError.getErrorCode(), coreApiError.getErrorCode());
            boolean messageMatches = Objects.equals(projectError.getMessage(), coreApiError.getMessage());
            boolean httpStatusCodeMatches = coreApiError.getHttpStatusCode() == projectError.getHttpStatusCode();
            if (errorCodeMatches && messageMatches && httpStatusCodeMatches) {
                return true;
            }
        }

        return false;
    }
}
