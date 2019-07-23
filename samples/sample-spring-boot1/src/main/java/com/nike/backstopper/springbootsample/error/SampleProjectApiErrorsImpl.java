package com.nike.backstopper.springbootsample.error;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRangeIntegerImpl;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

/**
 * Returns the project specific errors for this sample application. {@link #getProjectApiErrors()} will return a
 * combination of {@link SampleProjectApiErrorsBase#getCoreApiErrors()} and {@link #getProjectSpecificApiErrors()}.
 * This means that you have all the enum values of {@link com.nike.backstopper.apierror.sample.SampleCoreApiError}
 * and {@link SampleProjectApiError} at your disposal when throwing errors in this sample app.
 */
@Singleton
public class SampleProjectApiErrorsImpl extends SampleProjectApiErrorsBase {

    private static final List<ApiError> projectSpecificApiErrors =
        new ArrayList<>(Arrays.<ApiError>asList(SampleProjectApiError.values()));

    // Set the valid range of non-core error codes for this project to be 99100-99200.
    private static final ProjectSpecificErrorCodeRange errorCodeRange = new ProjectSpecificErrorCodeRangeIntegerImpl(
        99100, 99200, "SAMPLE_PROJECT_API_ERRORS"
    );

    @Override
    protected List<ApiError> getProjectSpecificApiErrors() {
        return projectSpecificApiErrors;
    }

    @Override
    protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
        return errorCodeRange;
    }

}
