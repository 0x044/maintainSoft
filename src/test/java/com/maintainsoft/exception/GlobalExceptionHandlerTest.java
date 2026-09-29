package com.maintainsoft.exception;

import com.maintainsoft.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ──────────────────────────────────────────────
    //  Handler method tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("handleDuplicateEmail")
    class HandleDuplicateEmailTests {

        @Test
        @DisplayName("should return CONFLICT (409) with exception message")
        void returnsConflictWithMessage() {
            String msg = "Email already exists: test@example.com";
            DuplicateEmailException ex = new DuplicateEmailException(msg);

            ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().error()).isEqualTo("Conflict");
            assertThat(response.getBody().message()).isEqualTo(msg);
            assertThat(response.getBody().timeStamp()).isNotNull();
        }

        @Test
        @DisplayName("should include a non-null timestamp in the response body")
        void timestampIsPresent() {
            DuplicateEmailException ex = new DuplicateEmailException("dup");

            ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().timeStamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleBadCredentials")
    class HandleBadCredentialsTests {

        @Test
        @DisplayName("should return UNAUTHORIZED (401) with fixed message")
        void returnsUnauthorizedWithFixedMessage() {
            BadCredentialsException ex = new BadCredentialsException("Bad creds");

            ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().error()).isEqualTo("Unauthorized");
            assertThat(response.getBody().message()).isEqualTo("Invalid username or password");
            assertThat(response.getBody().timeStamp()).isNotNull();
        }

        @Test
        @DisplayName("should NOT propagate the original exception message")
        void doesNotLeakOriginalMessage() {
            String originalMsg = "Secret internal error detail";
            BadCredentialsException ex = new BadCredentialsException(originalMsg);

            ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).doesNotContain(originalMsg);
        }
    }

    @Nested
    @DisplayName("handleBadToken")
    class HandleBadTokenTests {

        @Test
        @DisplayName("should return UNAUTHORIZED (401) with exception message")
        void returnsUnauthorizedWithExceptionMessage() {
            String msg = "Token has expired";
            InvalidTokenException ex = new InvalidTokenException(msg);

            ResponseEntity<ErrorResponse> response = handler.handleBadToken(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().error()).isEqualTo("Unauthorized");
            assertThat(response.getBody().message()).isEqualTo(msg);
            assertThat(response.getBody().timeStamp()).isNotNull();
        }

        @Test
        @DisplayName("should propagate the exact exception message")
        void propagatesExactMessage() {
            String msg = "Malformed JWT token";
            InvalidTokenException ex = new InvalidTokenException(msg);

            ResponseEntity<ErrorResponse> response = handler.handleBadToken(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo(msg);
        }
    }

    @Nested
    @DisplayName("handleNotFound")
    class HandleNotFoundTests {

        @Test
        @DisplayName("should return NOT_FOUND (404) with exception message")
        void returnsNotFoundWithExceptionMessage() {
            String msg = "User not found with id: 123";
            ResourceNotFoundException ex = new ResourceNotFoundException(msg);

            ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).isEqualTo(msg);
            assertThat(response.getBody().timeStamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleNoResourceFound")
    class HandleNoResourceFoundTests {

        @Test
        @DisplayName("should return NOT_FOUND (404) with exception message")
        void returnsNotFoundWithExceptionMessage() {
            org.springframework.web.servlet.resource.NoResourceFoundException ex =
                    org.mockito.Mockito.mock(org.springframework.web.servlet.resource.NoResourceFoundException.class);
            org.mockito.Mockito.when(ex.getMessage()).thenReturn("Resource not found");

            ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not Found");
            assertThat(response.getBody().message()).isEqualTo("Resource not found");
            assertThat(response.getBody().timeStamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleValidation")
    class HandleValidationTests {

        @Test
        @DisplayName("should return BAD_REQUEST with field validation messages")
        void returnsBadRequestWithFieldMessages() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad Request");
            assertThat(response.getBody().message()).contains("email", "must be a well-formed email address");
        }
    }

    @Nested
    @DisplayName("database conflict handlers")
    class DatabaseConflictHandlerTests {

        @Test
        @DisplayName("should map data integrity violations to a non-leaking 409")
        void mapsDataIntegrityViolationToConflict() {
            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                    new DataIntegrityViolationException("secret SQL detail")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).doesNotContain("secret SQL detail");
        }

        @Test
        @DisplayName("should map optimistic locking failures to a non-leaking 409")
        void mapsOptimisticLockingFailureToConflict() {
            ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                    new OptimisticLockingFailureException("secret lock detail")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).doesNotContain("secret lock detail");
        }
    }

    @Nested
    @DisplayName("handleAllErrors")
    class HandleAllErrorsTests {

        @Test
        @DisplayName("should return INTERNAL_SERVER_ERROR (500) with generic message")
        void returnsInternalServerErrorWithGenericMessage() {
            Exception ex = new Exception("Something unexpected");

            ResponseEntity<ErrorResponse> response = handler.handleAllErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Error");
            assertThat(response.getBody().message()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().timeStamp()).isNotNull();
        }

        @Test
        @DisplayName("should NOT leak the original exception message")
        void doesNotLeakOriginalMessage() {
            Exception ex = new Exception("SQL injection stack trace details");

            ResponseEntity<ErrorResponse> response = handler.handleAllErrors(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).doesNotContain("SQL injection");
            assertThat(response.getBody().message()).isEqualTo("Internal Server Error");
        }

        @Test
        @DisplayName("should handle NullPointerException as a generic error")
        void handlesNullPointerException() {
            NullPointerException ex = new NullPointerException("null ref");

            ResponseEntity<ErrorResponse> response = handler.handleAllErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
        }

        @Test
        @DisplayName("should handle RuntimeException as a generic error")
        void handlesRuntimeException() {
            RuntimeException ex = new RuntimeException("runtime");

            ResponseEntity<ErrorResponse> response = handler.handleAllErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────
    //  Custom exception class tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("DuplicateEmailException")
    class DuplicateEmailExceptionTests {

        @Test
        @DisplayName("should store the message via super(message)")
        void storesMessage() {
            String msg = "Email already registered";
            DuplicateEmailException ex = new DuplicateEmailException(msg);

            assertThat(ex.getMessage()).isEqualTo(msg);
        }

        @Test
        @DisplayName("should be a RuntimeException")
        void isRuntimeException() {
            DuplicateEmailException ex = new DuplicateEmailException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should handle null message")
        void handlesNullMessage() {
            DuplicateEmailException ex = new DuplicateEmailException(null);

            assertThat(ex.getMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("InvalidTokenException")
    class InvalidTokenExceptionTests {

        @Test
        @DisplayName("should store the message via super(message)")
        void storesMessage() {
            String msg = "Token expired";
            InvalidTokenException ex = new InvalidTokenException(msg);

            assertThat(ex.getMessage()).isEqualTo(msg);
        }

        @Test
        @DisplayName("should be a RuntimeException")
        void isRuntimeException() {
            InvalidTokenException ex = new InvalidTokenException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should handle empty message")
        void handlesEmptyMessage() {
            InvalidTokenException ex = new InvalidTokenException("");

            assertThat(ex.getMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("should store the message via super(message)")
        void storesMessage() {
            String msg = "Department not found";
            ResourceNotFoundException ex = new ResourceNotFoundException(msg);

            assertThat(ex.getMessage()).isEqualTo(msg);
        }

        @Test
        @DisplayName("should be a RuntimeException")
        void isRuntimeException() {
            ResourceNotFoundException ex = new ResourceNotFoundException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should handle null message")
        void handlesNullMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException(null);

            assertThat(ex.getMessage()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    //  Regression coverage for handler gaps
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("handleAuthentication")
    class HandleAuthenticationTests {

        @Test
        @DisplayName("a deleted user refreshing a still-valid token is 401, not 500")
        void usernameNotFoundIsUnauthorized() {
            UsernameNotFoundException ex = new UsernameNotFoundException("User not found: gone@example.com");

            ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody())
                    .isNotNull()
                    .satisfies(body -> {
                        assertThat(body.status()).isEqualTo(401);
                        // The response must not reveal that the account exists or not.
                        assertThat(body.message()).isEqualTo("Invalid username or password");
                    });
        }

        @Test
        @DisplayName("UsernameNotFoundException is an AuthenticationException")
        void usernameNotFoundIsAnAuthenticationException() {
            assertThat(new UsernameNotFoundException("x"))
                    .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("handleUnreadableBody")
    class HandleUnreadableBodyTests {

        @Test
        @DisplayName("malformed JSON is 400 and does not echo parser detail")
        void malformedJsonIsBadRequest() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Unexpected end-of-input in VALUE_STRING", new EmptyHttpInputMessage());

            ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .isNotNull()
                    .satisfies(body -> {
                        assertThat(body.status()).isEqualTo(400);
                        assertThat(body.message()).doesNotContain("VALUE_STRING");
                    });
        }
    }

    @Nested
    @DisplayName("handleMethodNotSupported")
    class HandleMethodNotSupportedTests {

        @Test
        @DisplayName("unsupported method is 405")
        void unsupportedMethodIsNotAllowed() {
            ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(
                    new org.springframework.web.HttpRequestMethodNotSupportedException("TRACE"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody())
                    .isNotNull()
                    .satisfies(body -> assertThat(body.status()).isEqualTo(405));
        }
    }

    private static final class EmptyHttpInputMessage
            implements org.springframework.http.HttpInputMessage {

        @Override
        public java.io.InputStream getBody() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return new org.springframework.http.HttpHeaders();
        }
    }
}
