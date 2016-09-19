package com.nike.backstopper.apierror.testutil;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;

import java.util.Arrays;
import java.util.List;

/**
 * An implementation of {@link ProjectApiErrors} intended to make unit and component testing easier. This uses {@link BarebonesCoreApiErrorForTesting}
 * values for the core errors and special error method return values. You can pass in whatever you want for {@link #getProjectSpecificApiErrors()}
 * and {@link #getProjectSpecificErrorCodeRange()} via the constructor (including null if that's what your test calls for).
 *
 * @author Nic Munroe
 */
public abstract class ProjectApiErrorsForTesting extends ProjectApiErrors {

    private static final List<ApiError> BAREBONES_CORE_API_ERRORS_AS_LIST = Arrays.<ApiError>asList(BarebonesCoreApiErrorForTesting.values());

    public static ProjectApiErrorsForTesting withProjectSpecificData(final List<ApiError> projectSpecificErrors,
                                                                     final ProjectSpecificErrorCodeRange projectSpecificErrorCodeRange) {
        return new ProjectApiErrorsForTesting() {
            @Override
            protected List<ApiError> getProjectSpecificApiErrors() {
                return projectSpecificErrors;
            }

            @Override
            protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
                return projectSpecificErrorCodeRange;
            }
        };
    }

    @Override
    protected List<ApiError> getCoreApiErrors() {
        return BAREBONES_CORE_API_ERRORS_AS_LIST;
    }

    @Override
    public ApiError getGenericServiceError() {
        return BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR;
    }

    @Override
    public ApiError getOusideDependencyReturnedAnUnrecoverableErrorApiError() {
        return BarebonesCoreApiErrorForTesting.OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR;
    }

    @Override
    public ApiError getServersideValidationApiError() {
        return BarebonesCoreApiErrorForTesting.SERVERSIDE_VALIDATION_ERROR;
    }

    @Override
    public ApiError getTemporaryServiceProblemApiError() {
        return BarebonesCoreApiErrorForTesting.TEMPORARY_SERVICE_PROBLEM;
    }

    @Override
    public ApiError getOutsideDependencyReturnedTemporaryErrorApiError() {
        return BarebonesCoreApiErrorForTesting.OUTSIDE_DEPENDENCY_RETURNED_A_TEMPORARY_ERROR;
    }

    @Override
    public ApiError getGenericBadRequestApiError() {
        return BarebonesCoreApiErrorForTesting.GENERIC_BAD_REQUEST;
    }

    @Override
    public ApiError getMissingExpectedContentApiError() {
        return BarebonesCoreApiErrorForTesting.MISSING_EXPECTED_CONTENT;
    }

    @Override
    public ApiError getTypeConversionApiError() {
        return BarebonesCoreApiErrorForTesting.TYPE_CONVERSION_ERROR;
    }

    @Override
    public ApiError getMalformedRequestApiError() {
        return BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST;
    }

    @Override
    public ApiError getUnauthorizedApiError() {
        return BarebonesCoreApiErrorForTesting.UNAUTHORIZED;
    }

    @Override
    public ApiError getForbiddenApiError() {
        return BarebonesCoreApiErrorForTesting.FORBIDDEN;
    }

    @Override
    public ApiError getNotFoundApiError() {
        return BarebonesCoreApiErrorForTesting.NOT_FOUND;
    }

    @Override
    public ApiError getMethodNotAllowedApiError() {
        return BarebonesCoreApiErrorForTesting.METHOD_NOT_ALLOWED;
    }

    @Override
    public ApiError getNoAcceptableRepresentationApiError() {
        return BarebonesCoreApiErrorForTesting.NO_ACCEPTABLE_REPRESENTATION;
    }

    @Override
    public ApiError getUnsupportedMediaTypeApiError() {
        return BarebonesCoreApiErrorForTesting.UNSUPPORTED_MEDIA_TYPE;
    }

    @Override
    public ApiError getTooManyRequestsApiError() {
        return BarebonesCoreApiErrorForTesting.TOO_MANY_REQUESTS;
    }
}
