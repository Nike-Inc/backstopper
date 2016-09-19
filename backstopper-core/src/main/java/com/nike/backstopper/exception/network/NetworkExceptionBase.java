package com.nike.backstopper.exception.network;

/**
 * Base class for network exceptions that occur (e.g. during HTTP client calls to other systems).
 *
 * @author Nic Munroe
 */
public abstract class NetworkExceptionBase extends RuntimeException {

    private final String connectionType;

    @SuppressWarnings("WeakerAccess")
    public NetworkExceptionBase(Throwable cause, String connectionType) {
        super(cause);
        this.connectionType = connectionType;
    }

    /**
     * @return The type of connection that failed. Answers the question: "what server was I trying to talk to when the
     *          exception occurred?". Useful in case the exception handler needs to know what server was being talked
     *          to in order to know how to process any response data contained in the exception, for example how to
     *          parse a HTTP Status Code 400 error for useful validation data to return to the end user.
     */
    public String getConnectionType() {
        return connectionType;
    }
}
