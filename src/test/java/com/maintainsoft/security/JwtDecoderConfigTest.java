package com.maintainsoft.security;

import com.maintainsoft.service.JwtService;
import com.maintainsoft.testsupport.SecurityConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDecoderConfigTest {

    private SecurityConfig securityConfig;
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void setUp() {
        securityConfig = SecurityConfigs.withThrowawayKeyPair();
        jwtEncoder = securityConfig.jwtEncoder();
    }

    @Test
    void accessDecoderAcceptsAccessTokensAndRejectsRefreshTokens() {
        JwtDecoder decoder = securityConfig.accessJwtDecoder();
        String accessToken = token("access");
        String refreshToken = token("refresh");

        assertThatCode(() -> decoder.decode(accessToken)).doesNotThrowAnyException();
        assertThatThrownBy(() -> decoder.decode(refreshToken))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void refreshDecoderAcceptsRefreshTokensAndRejectsAccessTokens() {
        JwtDecoder decoder = securityConfig.jwtDecoder();
        String accessToken = token("access");
        String refreshToken = token("refresh");

        assertThatCode(() -> decoder.decode(refreshToken)).doesNotThrowAnyException();
        assertThatThrownBy(() -> decoder.decode(accessToken))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void decoderRejectsWrongIssuer() {
        JwtDecoder decoder = securityConfig.accessJwtDecoder();
        String token = tokenWithIssuer("access", "https://wrong.example.com", Instant.now());

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void decoderRejectsExpiredToken() {
        JwtDecoder decoder = securityConfig.accessJwtDecoder();
        Instant now = Instant.now();
        String token = tokenWithIssuer("access", JwtService.JWT_ISSUER, now.minus(10, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void decoderRejectsTamperedSignature() {
        JwtDecoder decoder = securityConfig.accessJwtDecoder();
        String token = token("access");
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> decoder.decode(tampered))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    private String token(String type) {
        return tokenWithIssuer(type, JwtService.JWT_ISSUER, Instant.now());
    }

    private String tokenWithIssuer(String type, String issuer, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(5, ChronoUnit.MINUTES))
                .subject("user@example.com")
                .claim("type", type)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
