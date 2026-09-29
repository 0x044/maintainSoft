package com.maintainsoft.testsupport;

import com.maintainsoft.security.RsaKeyPair;
import com.maintainsoft.security.SecurityConfig;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Builds {@link SecurityConfig} instances for unit tests that need the configuration
 * class but do not exercise token verification against a real deployment key.
 */
public final class SecurityConfigs {

    private SecurityConfigs() {
    }

    public static SecurityConfig withThrowawayKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new SecurityConfig(new RsaKeyPair(
                    (RSAPublicKey) keyPair.getPublic(),
                    (RSAPrivateKey) keyPair.getPrivate()
            ));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA key generation is unavailable", exception);
        }
    }
}
