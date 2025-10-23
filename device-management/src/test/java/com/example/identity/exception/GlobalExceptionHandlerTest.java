package com.example.identity.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundBuilds404Response() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

        var response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Not found");
    }

    @Test
    void handleConflictBuilds409Response() {
        ConflictException ex = new ConflictException("Conflict");

        var response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Conflict");
    }

    @Test
    void handleValidationAggregatesErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", "must not be blank"));
        MethodParameter parameter;
        try {
            parameter = new MethodParameter(getClass().getDeclaredMethod("sample", String.class), 0);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<?, ?>) response.getBody().get("details")).containsKey("field");
    }

    @Test
    void handleIllegalArgumentBuildsBadRequestResponse() {
        IllegalArgumentException ex = new IllegalArgumentException("bad request");

        var response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "bad request");
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }
}
