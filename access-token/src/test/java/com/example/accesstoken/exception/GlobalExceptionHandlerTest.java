package com.example.accesstoken.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrorsAggregatesMessages() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "clientId", "must not be blank"));
        bindingResult.addError(new FieldError("request", "grantType", "must not be blank"));
        MethodParameter parameter;
        try {
            parameter = new MethodParameter(getClass().getDeclaredMethod("sample", String.class), 0);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat((List<?>) response.getBody().get("message")).hasSize(2);
    }

    @Test
    void handleInvalidClientBuildsUnauthorizedResponse() {
        InvalidClientException ex = new InvalidClientException("Invalid client");

        var response = handler.handleInvalidClient(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void handleInvalidTokenBuildsBadRequestResponse() {
        InvalidTokenException ex = new InvalidTokenException("Invalid token");

        var response = handler.handleInvalidToken(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_token");
    }

    @Test
    void handleIllegalArgumentBuildsBadRequestResponse() {
        IllegalArgumentException ex = new IllegalArgumentException("bad");

        var response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_request");
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }
}
