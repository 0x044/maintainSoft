package com.maintainsoft.security;

import com.maintainsoft.service.JwtService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({RsaKeyProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    private final RsaKeyPair rsaKeyPair;

    public SecurityConfig(RsaKeyPair rsaKeyPair) {
        this.rsaKeyPair = rsaKeyPair;
    }

    @Bean
    static RsaKeyPair rsaKeyPair(ResourceLoader resourceLoader, RsaKeyProperties properties) {
        return RsaKeyPairLoader.load(resourceLoader, properties);
    }

    /**
     * Throttles the anonymous auth endpoints before the controller runs.
     */
    @Bean
    AuthRateLimitFilter authRateLimitFilter(RateLimitProperties properties) {
        RateLimitProperties.Auth auth = properties.auth() == null
                ? new RateLimitProperties.Auth(10, 60, 30, 60, true)
                : properties.auth();

        return AuthRateLimitFilter.create(
                auth.loginPermits(),
                Duration.ofSeconds(auth.loginPeriodSeconds()),
                auth.refreshPermits(),
                Duration.ofSeconds(auth.refreshPeriodSeconds())
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger/**", "/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()

                        // Read access is available to any authenticated user.
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()

                        // Writes are restricted by role. Every mutating route must be
                        // listed here: the catch-all below denies by default, so a new
                        // endpoint cannot silently inherit authenticated access.
                        .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/machine-statuses")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/machines", "/api/v1/spares",
                                "/api/v1/spares/*/stock/*")
                        .hasAnyRole("MANAGER", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/machines/*", "/api/v1/spares/*")
                        .hasAnyRole("MANAGER", "SUPERVISOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/machines/*", "/api/v1/spares/*")
                        .hasAnyRole("MANAGER", "SUPERVISOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/repairs", "/api/v1/repairs/*/updates",
                                "/api/v1/repairs/*/costs")
                        .hasAnyRole("MANAGER", "SUPERVISOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/repairs/*/claim").hasRole("SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/repairs/*").hasAnyRole("MANAGER", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/repairs/*/assignment").hasRole("MANAGER")

                        // Department master data is manager-owned.
                        .requestMatchers(HttpMethod.POST, "/api/v1/departments").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/departments/*").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/*").hasRole("MANAGER")

                        .requestMatchers("/api/v1/auth/logout").authenticated()

                        // Deny by default: an unmapped API route is refused rather than
                        // silently inheriting authenticated access.
                        .requestMatchers("/api/v1/**").denyAll()
                        .anyRequest().denyAll()
                ).csrf(CsrfConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt((jwt) -> jwt
                        .decoder(accessJwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "Unauthorized",
                                        "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "Forbidden",
                                        "You do not have permission to perform this action")))
                .cors(Customizer.withDefaults());
        return http.build();
    }

    private void writeSecurityError(
            HttpServletResponse response,
            int status,
            String error,
            String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status
                + ",\"error\":\"" + error
                + "\",\"message\":\"" + message
                + "\",\"timeStamp\":\"" + Instant.now()
                + "\"}");
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost", "https://msi.leo-blenny.ts.net"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(rsaKeyPair.publicKey())
                .privateKey(rsaKeyPair.privateKey())
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return decoderForType("refresh");
    }

    @Bean
    JwtDecoder accessJwtDecoder() {
        return decoderForType("access");
    }

    private JwtDecoder decoderForType(String expectedType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKeyPair.publicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.JWT_ISSUER),
                tokenTypeValidator(expectedType)
        ));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> tokenTypeValidator(String expectedType) {
        return jwt -> expectedType.equals(jwt.getClaimAsString("type"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Expected token type: " + expectedType, null
        ));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}