package com.revplay.exception;

import com.revplay.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * A centralized exception handling component.
 * By using @RestControllerAdvice, this class intercepts exceptions thrown by
 * ANY controller
 * in the application. It formats the error into a consistent, user-friendly
 * JSON ApiResponse
 * instead of letting Spring return an ugly stack trace to the frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles cases where a requested item (like a Song, User, or Album) could not
     * be found in the database.
     * Returns a 404 Not Found HTTP status.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles general bad requests, such as invalid parameters or business logic
     * violations.
     * Returns a 400 Bad Request HTTP status.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles cases where a user tries to do something they have no permission for,
     * such as a Listener trying to upload a song or someone trying to access
     * without logging in.
     * Returns a 401 Unauthorized HTTP status.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles validation errors when incoming DTOs fail @Valid checks (e.g., empty
     * names, short passwords).
     * Extracts exactly which fields failed and maps them to human-readable error
     * messages.
     * Returns a 400 Bad Request HTTP status.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "Validation failed", errors));
    }

    /**
     * Handles the specific error thrown when a user tries to upload an audio file
     * or cover art
     * that is larger than the server's configured limits (usually defined in
     * application.properties).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiResponse(false, "File size exceeds maximum allowed size (50MB)"));
    }

    /**
     * Acts as an absolute fallback for any bug or error that we didn't explicitly
     * predict.
     * Prevents the server from crashing or exposing sensitive code internals.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "An unexpected error occurred: " + ex.getMessage()));
    }

    // ============================================
    // Custom exception definitions
    // These are simple classes used to throw specific error types that our handlers
    // above can catch.
    // ============================================

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
