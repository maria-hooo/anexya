package com.anexya.rfid_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TagNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, "RFID tag not found", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed: {}", errors);
        ApiError body = new ApiError(400, "Validation failed", "One or more fields are invalid", errors, Instant.now());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.BAD_REQUEST, "Malformed request",
                "Body is not valid JSON or a field has the wrong type (dates must be ISO-8601, e.g. 2026-10-10T08:30:00Z)");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, PropertyReferenceException.class})
    public ResponseEntity<ApiError> handleBadParameter(Exception ex) {
        log.warn("Bad request parameter: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "Invalid request parameter", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleEverythingElse(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            log.warn("Request rejected: {}", ex.getMessage());
            ProblemDetail pd = errorResponse.getBody();
            int status = errorResponse.getStatusCode().value();
            return ResponseEntity.status(status).body(ApiError.of(status, pd.getTitle(), pd.getDetail()));
        }
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), title, detail));
    }
}
