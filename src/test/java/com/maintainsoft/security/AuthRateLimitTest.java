package com.maintainsoft.security;

import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The auth endpoints are the only routes reachable without a token, so they are the
 * only place an unauthenticated caller can drive work against the system.
 *
 * <p>Limits are deliberately low so the behaviour is observable. Budgets are tracked per
 * client, and Resilience4j offers no way to reset a spent limiter, so each test uses its
 * own client address. That keeps the tests independent of execution order and exercises
 * the per-client isolation at the same time.
 */
@SpringBootTest(classes = AuthRateLimitTest.TestApplication.class,
        properties = {
                "app.rate-limit.auth.login-permits=3",
                "app.rate-limit.auth.login-period-seconds=300",
                "app.rate-limit.auth.refresh-permits=2",
                "app.rate-limit.auth.refresh-period-seconds=300"
        })
@AutoConfigureMockMvc
class AuthRateLimitTest {

    private static final String LOGIN_BODY =
            "{\"email\":\"victim@example.com\",\"password\":\"guess\"}";
    private static final String REFRESH_BODY = "{\"refreshToken\":\"a.b.c\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void repeatedLoginAttemptsAreThrottledAfterTheLimit() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("no"));

        // The first attempts still reach the controller, which proves the limiter
        // counts rather than blocking outright.
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(login("10.0.0.1"))
                    .andExpect(status().isUnauthorized());
        }

        var throttled = mockMvc.perform(login("10.0.0.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andReturn();

        // A throttled attempt must not reach the service: no password hashing, no write.
        verify(authService, times(3)).login(any());

        // The body must not echo the submitted identity or hint at the outcome.
        String body = throttled.getResponse().getContentAsString();
        assertThat(body).doesNotContain("victim@example.com");
        assertThat(body).contains("Too many attempts");
    }

    @Test
    void throttlingOneClientDoesNotAffectAnother() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("no"));

        // Exhaust this client's budget.
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(login("10.0.0.2")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login("10.0.0.2")).andExpect(status().isTooManyRequests());

        // A different client still has its full budget and is unaffected.
        mockMvc.perform(login("10.0.0.3")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIsThrottledOnItsOwnSeparateBudget() throws Exception {
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new com.maintainsoft.exception.InvalidTokenException("bad"));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(refresh("10.0.0.4")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(refresh("10.0.0.4"))
                .andExpect(status().isTooManyRequests());

        verify(authService, times(2)).refresh(any());
    }

    @Test
    void exhaustingLoginDoesNotConsumeTheRefreshBudget() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("no"));
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new com.maintainsoft.exception.InvalidTokenException("bad"));

        for (int attempt = 0; attempt < 6; attempt++) {
            mockMvc.perform(login("10.0.0.5"));
        }

        // Same client, different limiter, untouched budget.
        mockMvc.perform(refresh("10.0.0.5")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAuthRoutesDoNotConsumeTheAuthBudget() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("no"));

        // An unmapped route never reaches the limiter, so hammering it must not
        // throttle the login endpoint for the same client.
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/spares/not-an-auth-endpoint")
                            .remoteAddress("10.0.0.6")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        // The full login budget is still available for this client.
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(login("10.0.0.6")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login("10.0.0.6")).andExpect(status().isTooManyRequests());
    }

    private MockHttpServletRequestBuilder login(String clientAddress) {
        return post("/api/v1/auth/login")
                .remoteAddress(clientAddress)
                .contentType("application/json")
                .content(LOGIN_BODY);
    }

    private MockHttpServletRequestBuilder refresh(String clientAddress) {
        return post("/api/v1/auth/refresh")
                .remoteAddress(clientAddress)
                .contentType("application/json")
                .content(REFRESH_BODY);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            com.maintainsoft.controller.AuthController.class,
            com.maintainsoft.exception.GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}