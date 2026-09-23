package com.maintainsoft.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void mapsManagerScopeToSpringManagerRole() {
        Authentication authentication = convert("ROLE_MANAGER");

        assertThat(authentication).isNotNull();
        assertThat(authorities(authentication))
                .contains("ROLE_MANAGER")
                .doesNotContain("SCOPE_ROLE_MANAGER");
    }

    @Test
    void mapsSupervisorScopeToSpringSupervisorRole() {
        Authentication authentication = convert("ROLE_SUPERVISOR");

        assertThat(authentication).isNotNull();
        assertThat(authorities(authentication))
                .contains("ROLE_SUPERVISOR")
                .doesNotContain("SCOPE_ROLE_SUPERVISOR");
    }

    private Authentication convert(String scope) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user@example.com")
                .claim("scope", scope)
                .build();
        return securityConfig.jwtAuthenticationConverter().convert(jwt);
    }

    private static java.util.List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
