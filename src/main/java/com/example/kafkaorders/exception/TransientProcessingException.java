package com.example.kafkaorders.exception;

public class TransientProcessingException extends RuntimeException {
    public TransientProcessingException(String message) {
        super(message);
    }

    public TransientProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
