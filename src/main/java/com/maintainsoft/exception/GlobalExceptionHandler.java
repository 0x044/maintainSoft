package com.maintainsoft.exception;

import com.maintainsoft.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(401, "Unauthorized", "Invalid username or password", Instant.now()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ErrorResponse> handleBadToken(InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(401, "Unauthorized", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(404, "Not Found", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(404, "Not Found", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, "Bad Request", message, Instant.now())
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleAllErrors(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(500, "Error", "Internal Server Error", Instant.now())
        );
    }

    @ExceptionHandler(DuplicateDepartmentException.class)
    ResponseEntity<ErrorResponse> handleDuplicateDepartment(DuplicateDepartmentException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Duplicate", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(DuplicateMachineException.class)
    ResponseEntity<ErrorResponse> handleDuplicateMachine(DuplicateMachineException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(InvalidMachineException.class)
    ResponseEntity<ErrorResponse> handleInvalidMachine(InvalidMachineException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, "Bad Request", e.getMessage(), Instant.now())
        );
    }
}
