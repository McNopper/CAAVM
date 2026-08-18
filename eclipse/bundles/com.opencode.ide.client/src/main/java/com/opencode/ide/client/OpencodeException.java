package com.opencode.ide.client;

/**
 * Root exception for all opencode client failures (HTTP errors, malformed
 * responses, connection problems).
 */
public class OpencodeException extends Exception {
    private static final long serialVersionUID = 1L;

    public OpencodeException(String message) {
        super(message);
    }

    public OpencodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
