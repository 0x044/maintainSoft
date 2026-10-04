package com.maintainsoft.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Throttles the unauthenticated authentication endpoints.
 *
 * <p>Login is limited to slow password guessing, and refresh is limited to repeated
 * token validation against a database lookup. Limits are keyed per client address so a
 * single abusive client cannot consume the budget of everyone else.
 *
 * <p>Throttling happens before the controller, so a rejected attempt performs no
 * password hashing and writes nothing.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    static final String LOGIN_PATH = "/api/v1/auth/login";
    static final String REFRESH_PATH = "/api/v1/auth/refresh";
    static final String LOGIN_LIMITER = "auth-login";
    static final String REFRESH_LIMITER = "auth-refresh";

    private final RateLimiterRegistry registry;
    private final Map<String, RateLimiterConfig> configs;

    public AuthRateLimitFilter(RateLimiterRegistry registry) {
        this(registry, Map.of());
    }

    public AuthRateLimitFilter(RateLimiterRegistry registry, Map<String, RateLimiterConfig> configs) {
        this.registry = registry;
        this.configs = Map.copyOf(configs);
    }

    /**
     * Builds a registry whose limiters are keyed by client address.
     *
     * @param loginPermits   login attempts allowed per client per period
     * @param loginPeriod    login window length
     * @param refreshPermits refresh attempts allowed per client per period
     * @param refreshPeriod  refresh window length
     */
    public static AuthRateLimitFilter create(
            long loginPermits, Duration loginPeriod,
            long refreshPermits, Duration refreshPeriod) {

        RateLimiterConfig loginConfig = config(loginPermits, loginPeriod);
        RateLimiterConfig refreshConfig = config(refreshPermits, refreshPeriod);

        // The registry default only applies to names that are never configured. Every
        // per-client limiter below is created with its own configuration, because a name
        // such as "auth-refresh:10.0.0.1" is not pre-registered and would otherwise
        // silently inherit the login limit.
        RateLimiterRegistry registry = RateLimiterRegistry.of(loginConfig);
        return new AuthRateLimitFilter(registry, Map.of(
                LOGIN_LIMITER, loginConfig,
                REFRESH_LIMITER, refreshConfig));
    }

    private static RateLimiterConfig config(long permits, Duration period) {
        return RateLimiterConfig.custom()
                .limitForPeriod((int) Math.max(1, Math.min(permits, Integer.MAX_VALUE)))
                .limitRefreshPeriod(period)
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isPermitted(request)) {
            log.warn("Rate limit exceeded for {} on {}", request.getRemoteAddr(), request.getRequestURI());
            writeRateLimited(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPermitted(HttpServletRequest request) {
        String limiterName = limiterFor(request);
        if (limiterName == null) {
            return true;
        }
        RateLimiterConfig config = configs.get(limiterName);
        RateLimiter limiter = registry.rateLimiter(
                clientKey(limiterName, clientAddress(request)), config);
        return limiter.acquirePermission(1);
    }

    /**
     * Returns the limiter name for a throttled request, or {@code null} when the
     * request is not a rate-limited one.
     */
    static String limiterFor(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (LOGIN_PATH.equals(path)) {
            return LOGIN_LIMITER;
        }
        if (REFRESH_PATH.equals(path)) {
            return REFRESH_LIMITER;
        }
        return null;
    }

    private static String clientKey(String limiterName, String remoteAddress) {
        return limiterName + ":" + remoteAddress;
    }

    /**
     * Identifies the calling client.
     *
     * <p>Behind a reverse proxy {@code getRemoteAddr()} is the proxy itself, which would
     * merge every user into one bucket. {@code X-Forwarded-For} is therefore consulted
     * when present, since the deployment is expected to terminate TLS at a trusted
     * proxy that sets it. Deployments without a proxy must strip this header at the
     * edge, otherwise a caller could rotate a forged address to bypass the limit.
     */
    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // A fixed body matching the ErrorResponse shape used everywhere else, and
        // deliberately identical for every cause so nothing is disclosed.
        response.getWriter().write("{\"status\":429"
                + ",\"error\":\"Too Many Requests\""
                + ",\"message\":\"Too many attempts. Please try again later.\""
                + ",\"timeStamp\":\"" + Instant.now() + "\"}");
    }
}