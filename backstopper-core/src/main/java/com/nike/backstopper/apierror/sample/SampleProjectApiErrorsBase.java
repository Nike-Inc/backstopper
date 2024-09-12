package com.nike.backstopper.apierror.sample;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import java.util.Arrays;
import java.util.List;

/**
 * A sample/example {@link ProjectApiErrors} that uses {@link SampleCoreApiError} values for
 * {@link #getCoreApiErrors()} and the various {@code get[Special]ApiError()} methods. The only things concrete
 * extensions will need to implement is {@link #getProjectSpecificApiErrors()} and
 * {@link #getProjectSpecificErrorCodeRange()}.
 *
 * <p>This is only suitable for production usage if the error codes and messages associated with the
 * {@link SampleCoreApiError} values are fine for your project. In practice most organizations should copy/paste
 * {@link SampleCoreApiError} and customize the error codes and messages for their organization, and then copy/paste
 * this base class to use their organization's new set of core errors. If the organization-specific version of
 * {@link ProjectApiErrors} is published in a reusable library then it can be shared around to all projects that should
 * use the same values for their core errors and all they will have to do is implement
 * {@link #getProjectSpecificApiErrors()} and {@link #getProjectSpecificErrorCodeRange()}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class SampleProjectApiErrorsBase extends ProjectApiErrors {

    private static final List<ApiError> SAMPLE_CORE_API_ERRORS_AS_LIST =
        Arrays.asList(SampleCoreApiError.values());

    @Override
    protected List<ApiError> getCoreApiErrors() {
        return SAMPLE_CORE_API_ERRORS_AS_LIST;
    }

    @Override
    public ApiError getGenericServiceError() {
        return SampleCoreApiError.GENERIC_SERVICE_ERROR;
    }

    @Override
    public ApiError getOusideDependencyReturnedAnUnrecoverableErrorApiError() {
        return SampleCoreApiError.OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR;
    }

    @Override
    public ApiError getServersideValidationApiError() {
        return SampleCoreApiError.SERVERSIDE_VALIDATION_ERROR;
    }

    @Override
    public ApiError getTemporaryServiceProblemApiError() {
        return SampleCoreApiError.TEMPORARY_SERVICE_PROBLEM;
    }

    @Override
    public ApiError getOutsideDependencyReturnedTemporaryErrorApiError() {
        return SampleCoreApiError.OUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERROR;
    }

    @Override
    public ApiError getGenericBadRequestApiError() {
        return SampleCoreApiError.INVALID_REQUEST;
    }

    @Override
    public ApiError getMissingExpectedContentApiError() {
        return SampleCoreApiError.MISSING_EXPECTED_CONTENT;
    }

    @Override
    public ApiError getTypeConversionApiError() {
        return SampleCoreApiError.TYPE_CONVERSION_ERROR;
    }

    @Override
    public ApiError getMalformedRequestApiError() {
        return SampleCoreApiError.MALFORMED_REQUEST;
    }

    @Override
    public ApiError getUnauthorizedApiError() {
        return SampleCoreApiError.UNAUTHORIZED;
    }

    @Override
    public ApiError getForbiddenApiError() {
        return SampleCoreApiError.FORBIDDEN;
    }

    @Override
    public ApiError getNotFoundApiError() {
        return SampleCoreApiError.NOT_FOUND;
    }

    @Override
    public ApiError getMethodNotAllowedApiError() {
        return SampleCoreApiError.METHOD_NOT_ALLOWED;
    }

    @Override
    public ApiError getNoAcceptableRepresentationApiError() {
        return SampleCoreApiError.NO_ACCEPTABLE_REPRESENTATION;
    }

    @Override
    public ApiError getUnsupportedMediaTypeApiError() {
        return SampleCoreApiError.UNSUPPORTED_MEDIA_TYPE;
    }

    @Override
    public ApiError getTooManyRequestsApiError() {
        return SampleCoreApiError.TOO_MANY_REQUESTS;
    }
}
