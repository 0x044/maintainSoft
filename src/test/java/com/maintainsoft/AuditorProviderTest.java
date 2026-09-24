package com.maintainsoft;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuditorProviderTest {

    private final MaintainsoftApplication application = new MaintainsoftApplication();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousAuthenticationFallsBackToSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                )
        );

        Optional<String> auditor = application.auditorProvider().getCurrentAuditor();

        assertThat(auditor).contains("system");
    }

    @Test
    void authenticatedPrincipalIsUsedAsAuditor() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "supervisor@example.com", "ignored", AuthorityUtils.NO_AUTHORITIES
                )
        );

        assertThat(application.auditorProvider().getCurrentAuditor())
                .contains("supervisor@example.com");
    }
}
