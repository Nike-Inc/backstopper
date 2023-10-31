package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiErrorValue;

import java.io.Serializable;
import java.util.Objects;

/**
 * The immutable model is used to represent {@link ApiErrorValue} metadata.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadata implements Serializable {

    private static final long serialVersionUID = 2L;

    private final String errorCode;
    private final int httpStatusCode;
    private final String message;

    public ApiErrorValueMetadata(String errorCode,
                                 int httpStatusCode,
                                 String message) {
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiErrorValueMetadata that = (ApiErrorValueMetadata) o;
        return Objects.equals(errorCode, that.errorCode)
                && Objects.equals(httpStatusCode, that.httpStatusCode)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, httpStatusCode, message);
    }

}
