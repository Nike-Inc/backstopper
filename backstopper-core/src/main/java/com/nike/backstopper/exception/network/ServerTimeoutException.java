package com.nike.backstopper.exception.network;

/**
 * Indicates that the call to the downstream server was taking too long and was killed.
 *
 * @author Nic Munroe
 */
public class ServerTimeoutException extends NetworkExceptionBase {
    public ServerTimeoutException(Throwable cause, String connectionType) {
        super(cause, connectionType);
    }
}
