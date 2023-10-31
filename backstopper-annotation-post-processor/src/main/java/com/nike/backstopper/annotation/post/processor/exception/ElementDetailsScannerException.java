package com.nike.backstopper.annotation.post.processor.exception;

import javax.lang.model.element.Element;

/**
 * This exception is used to signal {@link Element} scanning issues.
 *
 * @author Andrey Tsarenko
 */
public class ElementDetailsScannerException extends RuntimeException {

    public ElementDetailsScannerException(String message, Throwable cause) {
        super(message, cause);
    }

}
