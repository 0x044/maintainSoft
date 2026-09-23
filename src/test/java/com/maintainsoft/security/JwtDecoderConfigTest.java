package com.maintainsoft.security;

import com.maintainsoft.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDecoderConfigTest {

    private SecurityConfig securityConfig;
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        securityConfig = new SecurityConfig();
        securityConfig.key = (java.security.interfaces.RSAPublicKey) keyPair.getPublic();
        securityConfig.privateKey = (java.security.interfaces.RSAPrivateKey) keyPair.getPrivate();
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

    private String token(String type) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtService.JWT_ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .subject("user@example.com")
                .claim("type", type)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
