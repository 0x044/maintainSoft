package com.maintainsoft.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Unit Tests")
class HealthControllerTest {

    @InjectMocks
    private HealthController healthController;

    @Test
    @DisplayName("Should return OK (200) with status UP and authenticated user name")
    void health_returnsOk_withStatusUpAndUserName() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@example.com");

        // Act
        ResponseEntity<Map<String, String>> result = healthController.health(auth);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).containsEntry("status", "UP");
        assertThat(result.getBody()).containsEntry("user", "admin@example.com");
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("Should return correct user name from authentication")
    void health_returnsCorrectUserName_fromAuthentication() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("balu@softex.com");

        // Act
        ResponseEntity<Map<String, String>> result = healthController.health(auth);

        // Assert
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("user")).isEqualTo("balu@softex.com");
    }

    @Test
    @DisplayName("Should always include status UP regardless of user")
    void health_alwaysIncludesStatusUp() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("anyuser@test.com");

        // Act
        ResponseEntity<Map<String, String>> result = healthController.health(auth);

        // Assert
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("status")).isEqualTo("UP");
    }
}
