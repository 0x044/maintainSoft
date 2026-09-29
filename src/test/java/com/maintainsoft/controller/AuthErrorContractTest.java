package com.maintainsoft.controller;

import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.exception.GlobalExceptionHandler;
import com.maintainsoft.security.SecurityConfig;
import com.maintainsoft.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error-response contract for the authentication routes, exercised through the real
 * exception handler.
 *
 * <p>These cases are easy to get wrong in a way no unit test would catch: an
 * unhandled exception type falls through to the catch-all and turns a client mistake
 * into a 500, or leaks internals into the response body.
 */
@SpringBootTest(classes = AuthErrorContractTest.TestApplication.class)
@AutoConfigureMockMvc
class AuthErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void refreshForDeletedUserIsUnauthorizedRatherThanServerError() throws Exception {
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "User not found: gone@example.com"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"a.b.c\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithUnknownUserIsUnauthorizedRatherThanServerError() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "User not found: ghost@example.com"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedJsonIsABadRequestRatherThanAServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"unterminated"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void emptyBodyIsABadRequestRatherThanAServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validationFailureIsABadRequestAndNamesTheField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void unsupportedMethodIsNotAllowedRatherThanAServerError() throws Exception {
        // The security filter chain runs before method dispatch, so the caller must be
        // authenticated for the request to reach the handler under test.
        mockMvc.perform(put("/api/v1/auth/login")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unexpectedServerFailureIsLoggedAndDoesNotLeakDetail() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/db relation missing"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-host"))));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
    })
    @Import({SecurityConfig.class, AuthController.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }
}
