package com.maintainsoft.exception;

/**
 * The requested change would leave the system in an unusable or unsafe state, for
 * example deactivating the last remaining manager.
 */
public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String message) {
        super(message);
    }
}