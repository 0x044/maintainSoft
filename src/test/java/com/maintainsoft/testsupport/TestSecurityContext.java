package com.maintainsoft.testsupport;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * Helpers for populating the security context in tests.
 *
 * <p>With method security enabled the service layer reads the caller's authorities from
 * the security context, so an integration test that exercises a restricted operation
 * must present a role the way a real request would.
 */
public final class TestSecurityContext {

    public static final String TEST_EMAIL = "integration@example.com";

    private TestSecurityContext() {
    }

    public static Authentication asManager() {
        return as("ROLE_MANAGER");
    }

    public static Authentication asSupervisor() {
        return as("ROLE_SUPERVISOR");
    }

    public static Authentication as(String authority) {
        return new UsernamePasswordAuthenticationToken(
                TEST_EMAIL,
                "ignored",
                java.util.List.of(new SimpleGrantedAuthority(authority))
        );
    }

    /**
     * Runs the supplied action with the given authentication installed, and restores the
     * previous context afterwards so one test cannot affect the next.
 */
    public static void runAs(Authentication authentication, Runnable action) {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    /**
     * Value-returning variant of {@link #runAs(Authentication, Runnable)}.
     */
    public static <T> T runAs(Authentication authentication, Supplier<T> action) {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    public static <T> T runAsManager(Supplier<T> action) {
        return runAs(asManager(), action);
    }
}