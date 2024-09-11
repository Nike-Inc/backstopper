package testonly.componenttest.spring.reusable.error;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRangeIntegerImpl;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Singleton;

/**
 * Returns the project specific errors for the {@code testonly-spring*} component tests.
 */
@Singleton
public class SampleProjectApiErrorsImpl extends SampleProjectApiErrorsBase {

    private static final List<ApiError> projectSpecificApiErrors =
        new ArrayList<>(Arrays.<ApiError>asList(SampleProjectApiError.values()));

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
