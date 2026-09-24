package com.maintainsoft.controller;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.LogoutRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    @DisplayName("Should not expose the public registration route")
    void registerEndpointIsNotExposed() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        mockMvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Should reject invalid login input before calling the service")
    void invalidLoginInputIsRejectedAtControllerBoundary() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController)
                    .setValidator(validator)
                    .build();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\",\"password\":\" \"}"))
                    .andExpect(status().isBadRequest());
        } finally {
            validator.destroy();
        }

        verifyNoInteractions(authService);
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

    @Test
    @DisplayName("POST /api/v1/auth/logout returns 204")
    void logoutReturnsNoContent() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        ResponseEntity<Void> result = authController.logout(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout(request);
    }
}
