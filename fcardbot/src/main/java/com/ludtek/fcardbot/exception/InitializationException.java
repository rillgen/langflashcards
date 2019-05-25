package com.ludtek.fcardbot.exception;

/**
 * Thrown when it is not possible to initialize the application
 */
public class InitializationException extends RuntimeException {

    public InitializationException(String message) {
        super(message);
    }
}
