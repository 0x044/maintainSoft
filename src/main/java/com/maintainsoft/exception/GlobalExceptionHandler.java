package com.maintainsoft.exception;

import com.maintainsoft.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(401, "Unauthorized", "Invalid username or password", Instant.now()));
    }

    /**
     * A refresh token can outlive the account that owns it, so loading the user is a
     * step where an otherwise valid token can fail. That is an authentication failure,
     * not a server fault: without this handler the catch-all would report 500 for a
     * routine condition and tell an operator the service is broken.
     */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(401, "Unauthorized", "Invalid username or password", Instant.now())
        );
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, "Bad Request", "Request body is missing or malformed", Instant.now())
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                new ErrorResponse(405, "Method Not Allowed", "Request method is not supported for this resource",
                        Instant.now())
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleAllErrors(Exception e) {
        // An unexpected exception is a defect, so it must be diagnosable from the logs
        // rather than being converted into a silent, generic 500. The response body
        // stays generic so internal details are never leaked to the caller.
        log.error("Unhandled exception while processing a request", e);

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

    @ExceptionHandler(DuplicateSpareException.class)
    ResponseEntity<ErrorResponse> handleDuplicateSpare(DuplicateSpareException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(InvalidSpareException.class)
    ResponseEntity<ErrorResponse> handleInvalidSpare(InvalidSpareException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, "Bad Request", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(DuplicateRepairException.class)
    ResponseEntity<ErrorResponse> handleDuplicateRepair(DuplicateRepairException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(InvalidRepairException.class)
    ResponseEntity<ErrorResponse> handleInvalidRepair(InvalidRepairException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(400, "Bad Request", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(RepairForbiddenException.class)
    ResponseEntity<ErrorResponse> handleRepairForbidden(RepairForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(403, "Forbidden", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(RepairConflictException.class)
    ResponseEntity<ErrorResponse> handleRepairConflict(RepairConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", e.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", "The requested change conflicts with existing data", Instant.now())
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", "The record changed while the request was being processed", Instant.now())
        );
    }
}
