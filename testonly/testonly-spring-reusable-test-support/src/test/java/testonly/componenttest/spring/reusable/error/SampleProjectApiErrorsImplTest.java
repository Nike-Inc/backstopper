package testonly.componenttest.spring.reusable.error;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrorsTestBase;

/**
 * Extends {@link ProjectApiErrorsTestBase} in order to inherit tests that will verify the correctness of
 * {@link SampleProjectApiErrorsImpl}.
 *
 * @author Nic Munroe
 */
public class SampleProjectApiErrorsImplTest extends ProjectApiErrorsTestBase {

    private final ProjectApiErrors projectApiErrors = new SampleProjectApiErrorsImpl();

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return projectApiErrors;
    }
}