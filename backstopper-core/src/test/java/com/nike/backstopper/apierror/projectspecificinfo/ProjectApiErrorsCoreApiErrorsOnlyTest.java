package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;

/**
 * Extension of {@link ProjectApiErrorsTestBase} that tests the case where the {@link ProjectApiErrors} doesn't have
 * any project-specific ApiErrors and thus only contains core errors.
 *
 * @author Nic Munroe
 */
public class ProjectApiErrorsCoreApiErrorsOnlyTest extends ProjectApiErrorsTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return testProjectApiErrors;
    }
}
