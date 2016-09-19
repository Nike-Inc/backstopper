package com.nike.backstopper.exception.network;

/**
 * Indicates that the downstream server could not be communicated with <b>at all</b> for some reason.
 *
 * @author Nic Munroe
 */
public class ServerUnreachableException extends NetworkExceptionBase {
    public ServerUnreachableException(Throwable cause, String connectionType) {
        super(cause, connectionType);
    }
}
