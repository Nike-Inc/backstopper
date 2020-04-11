package com.nike.backstopper.spring.processor.exception;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

/**
 * This exception is used to signal {@link ProjectApiErrors} processing issues.
 *
 * @author Andrey Tsarenko
 */
public class ProjectApiErrorsBeanPostProcessorException extends RuntimeException {

    public ProjectApiErrorsBeanPostProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

}
