package com.maintainsoft.controller;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.dto.RegisterRequest;
import com.maintainsoft.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse buildAuthResponse() {
        return new AuthResponse(
                "access-token-123",
                "refresh-token-456",
                "user@example.com",
                "MANAGER"
        );
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should return CREATED (201) status with auth response")
        void register_returnsCreatedStatus_withAuthResponse() {
            // Arrange
            UUID departmentId = UUID.randomUUID();
            RegisterRequest request = new RegisterRequest(
                    "John Doe", "john@example.com", "password123", "9876543210", departmentId
            );
            AuthResponse expectedResponse = buildAuthResponse();
            when(authService.register(request)).thenReturn(expectedResponse);

            // Act
            ResponseEntity<AuthResponse> result = authController.register(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().accessToken()).isEqualTo("access-token-123");
            assertThat(result.getBody().refreshToken()).isEqualTo("refresh-token-456");
            assertThat(result.getBody().email()).isEqualTo("user@example.com");
            assertThat(result.getBody().role()).isEqualTo("MANAGER");
            verify(authService, times(1)).register(request);
        }

        @Test
        @DisplayName("Should delegate to AuthService exactly once")
        void register_delegatesToAuthService() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "Jane", "jane@example.com", "pass", "1234567890", UUID.randomUUID()
            );
            when(authService.register(request)).thenReturn(buildAuthResponse());

            // Act
            authController.register(request);

            // Assert
            verify(authService, times(1)).register(request);
            verifyNoMoreInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return OK (200) status with auth response")
        void login_returnsOkStatus_withAuthResponse() {
            // Arrange
            LoginRequest request = new LoginRequest("john@example.com", "password123");
            AuthResponse expectedResponse = buildAuthResponse();
            when(authService.login(request)).thenReturn(expectedResponse);

            // Act
            ResponseEntity<AuthResponse> result = authController.login(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().accessToken()).isEqualTo("access-token-123");
            assertThat(result.getBody().refreshToken()).isEqualTo("refresh-token-456");
            assertThat(result.getBody().email()).isEqualTo("user@example.com");
            assertThat(result.getBody().role()).isEqualTo("MANAGER");
            verify(authService, times(1)).login(request);
        }

        @Test
        @DisplayName("Should propagate exception from AuthService")
        void login_whenServiceThrows_propagatesException() {
            // Arrange
            LoginRequest request = new LoginRequest("bad@example.com", "wrong");
            when(authService.login(request)).thenThrow(new RuntimeException("Invalid credentials"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> authController.login(request));
            verify(authService).login(request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("Should return OK (200) status with new auth response")
        void refresh_returnsOkStatus_withAuthResponse() {
            // Arrange
            RefreshRequest request = new RefreshRequest("valid-refresh-token");
            AuthResponse expectedResponse = new AuthResponse(
                    "new-access-token", "new-refresh-token", "user@example.com", "SUPERVISOR"
            );
            when(authService.refresh(request)).thenReturn(expectedResponse);

            // Act
            ResponseEntity<AuthResponse> result = authController.refresh(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().accessToken()).isEqualTo("new-access-token");
            assertThat(result.getBody().refreshToken()).isEqualTo("new-refresh-token");
            assertThat(result.getBody().email()).isEqualTo("user@example.com");
            assertThat(result.getBody().role()).isEqualTo("SUPERVISOR");
            verify(authService, times(1)).refresh(request);
        }

        @Test
        @DisplayName("Should propagate exception when refresh token is invalid")
        void refresh_whenServiceThrows_propagatesException() {
            // Arrange
            RefreshRequest request = new RefreshRequest("expired-token");
            when(authService.refresh(request)).thenThrow(new RuntimeException("Token expired"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> authController.refresh(request));
            verify(authService).refresh(request);
        }
    }
}
