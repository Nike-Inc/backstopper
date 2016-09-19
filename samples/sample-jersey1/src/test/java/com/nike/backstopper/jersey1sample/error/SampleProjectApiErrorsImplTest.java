package com.nike.backstopper.jersey1sample.error;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrorsTestBase;

/**
 * Extends {@link ProjectApiErrorsTestBase} in order to inherit tests that will verify the correctness of this
 * project's {@link SampleProjectApiErrorsImpl}.
 *
 * @author Nic Munroe
 */
public class SampleProjectApiErrorsImplTest extends ProjectApiErrorsTestBase {

    ProjectApiErrors projectApiErrors = new SampleProjectApiErrorsImpl();

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return projectApiErrors;
    }
}