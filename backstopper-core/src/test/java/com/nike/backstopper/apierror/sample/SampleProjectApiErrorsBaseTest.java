package com.nike.backstopper.apierror.sample;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrorsTestBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;

import java.util.List;

/**
 * Extension of {@link ProjectApiErrorsTestBase} that tests {@link SampleProjectApiErrorsBase}.
 *
 * @author Nic Munroe
 */
public class SampleProjectApiErrorsBaseTest extends ProjectApiErrorsTestBase {

    private static final ProjectApiErrors testProjectApiErrors = new SampleProjectApiErrorsBase() {
        @Override
        protected List<ApiError> getProjectSpecificApiErrors() {
            return null;
        }

        @Override
        protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
            return null;
        }
    };

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return testProjectApiErrors;
    }
}
