package com.maintainsoft.exception;

import com.maintainsoft.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

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
}
