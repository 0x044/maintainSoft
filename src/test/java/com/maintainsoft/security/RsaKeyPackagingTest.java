package com.maintainsoft.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the decision to keep signing keys out of the application classpath.
 *
 * <p>A private key under {@code src/main/resources} is packaged into the JAR and
 * shipped to everyone who can download the artifact, which a later commit cannot
 * undo. These tests fail if key files reappear on the runtime classpath, or if key
 * loading ever stops failing loudly when the configured file is absent.
 */
class RsaKeyPackagingTest {

    @Test
    void noKeyMaterialIsPackagedOnTheClasspath() {
        assertThat(new ClassPathResource("private.key").exists())
                .as("private.key must not be packaged with the application")
                .isFalse();
        assertThat(new ClassPathResource("public.key").exists())
                .as("public.key must not be packaged with the application")
                .isFalse();
    }

    @Test
    void missingKeyFileFailsWithAnActionableMessage() {
        RsaKeyProperties properties = new RsaKeyProperties(
                "classpath:keys/does-not-exist.key",
                "classpath:keys/does-not-exist.key"
        );

        assertThatThrownBy(() -> RsaKeyPairLoader.load(new DefaultResourceLoader(), properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found")
                .hasMessageContaining("scripts/generate-jwt-keys.sh");
    }

    @Test
    void unconfiguredKeyFailsWithAnActionableMessage() {
        RsaKeyProperties properties = new RsaKeyProperties(null, null);

        assertThatThrownBy(() -> RsaKeyPairLoader.load(new DefaultResourceLoader(), properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No RSA public key is configured");
    }
}
