package com.turbotoggle.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Handles Optimistic Locking / Concurrent update conflicts (HTTP 409)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException e){
        return  ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "CONFILCT",
                "message", "This flag was modified by another request. Please fetch the latest version and try again."
        ));
    }

    // Handles Rule Validation errors (HTTP 400)
    @ExceptionHandler(InvalidRuleConfigurationException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRuleConfiguration(InvalidRuleConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage() // ✅ Matches jsonPath("$.error") in test assertion
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
