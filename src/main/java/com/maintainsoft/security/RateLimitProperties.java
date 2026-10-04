package com.maintainsoft.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Throttling limits for the unauthenticated authentication endpoints.
 *
 * <p>Login and refresh accept anonymous callers, so they are the only place where an
 * unauthenticated attacker can drive work against the system: by brute-forcing
 * credentials or by forcing repeated token validation. Both are limited here.
 *
 * <p>The default of 10 attempts per minute is deliberately far above normal use, and it
 * is per client address rather than global so one noisy client cannot lock everyone out.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Auth auth) {

    public record Auth(
            long loginPermits,
            long loginPeriodSeconds,
            long refreshPermits,
            long refreshPeriodSeconds,
            boolean enabled
    ) {
    }
}