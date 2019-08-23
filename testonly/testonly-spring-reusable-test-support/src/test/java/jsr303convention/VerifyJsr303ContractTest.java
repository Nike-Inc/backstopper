package jsr303convention;

import com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase;
import com.nike.backstopper.apierror.contract.jsr303convention.VerifyJsr303ValidationMessagesPointToApiErrorsTest;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;

/**
 * Verifies that *ALL* non-excluded JSR 303 validation annotations in this project have a message defined that maps to a
 * {@link com.nike.backstopper.apierror.ApiError} enum name from this project's {@link SampleProjectApiErrorsImpl}.
 */
public class VerifyJsr303ContractTest extends VerifyJsr303ValidationMessagesPointToApiErrorsTest {

    private static final ProjectApiErrors PROJECT_API_ERRORS = new SampleProjectApiErrorsImpl();

    @Override
    protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
        return ApplicationJsr303AnnotationTroller.getInstance();
    }

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return PROJECT_API_ERRORS;
    }
}
