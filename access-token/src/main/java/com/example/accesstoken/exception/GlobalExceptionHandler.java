package com.example.accesstoken.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "validation_failed");
        body.put("message", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidClientException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidClient(InvalidClientException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_client", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("invalid_token", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(error("invalid_request", ex.getMessage()));
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
