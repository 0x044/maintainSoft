package com.maintainsoft.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigurationTest {

    @Test
    void allowsBrowserMethodsAndHeadersRequiredByTheApi() {
        CorsConfiguration configuration = new SecurityConfig()
                .corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/departments"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .contains("Authorization", "Content-Type", "Accept", "Origin");
    }
}
