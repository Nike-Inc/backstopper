package com.nike.backstopper.jersey2sample.error;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrorsTestBase;
import com.nike.backstopper.jersey2sample.error.SampleProjectApiErrorsImpl;

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