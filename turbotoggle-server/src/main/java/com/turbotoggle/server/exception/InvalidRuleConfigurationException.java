package com.turbotoggle.server.exception;

public class InvalidRuleConfigurationException extends RuntimeException {
    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}