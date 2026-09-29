package com.maintainsoft.security;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Reads the PEM-encoded RSA key pair configured through {@link RsaKeyProperties}.
 *
 * <p>Key material is deliberately loaded from an external location so that no private
 * key is packaged inside the application JAR.
 */
public final class RsaKeyPairLoader {

    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";

    private RsaKeyPairLoader() {
    }

    public static RsaKeyPair load(ResourceLoader resourceLoader, RsaKeyProperties properties) {
        return new RsaKeyPair(
                parsePublicKey(read(resourceLoader, properties.publicKey(), "public")),
                parsePrivateKey(read(resourceLoader, properties.privateKey(), "private"))
        );
    }

    private static String read(ResourceLoader resourceLoader, String location, String type) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("No RSA " + type + " key is configured. Set app.security.rsa."
                    + type + "-key to the location of a PEM encoded key file.");
        }

        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "The configured RSA " + type + " key was not found at " + location
                            + ". Run scripts/generate-jwt-keys.sh to create a local key pair.");
        }

        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read the configured RSA " + type + " key from " + location, exception);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        byte[] encoded = decode(pem, PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER, "public");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception exception) {
            throw new IllegalStateException("The configured RSA public key is not valid", exception);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        byte[] encoded = decode(pem, PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER, "private");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception exception) {
            throw new IllegalStateException("The configured RSA private key is not valid", exception);
        }
    }

    private static byte[] decode(String pem, String header, String footer, String type) {
        String normalized = pem.trim().replace("\r\n", "\n");
        if (!normalized.startsWith(header) || !normalized.endsWith(footer)) {
            throw new IllegalStateException("The configured RSA " + type
                    + " key is not a PEM encoded " + header + " block");
        }

        String base64 = normalized.substring(header.length(), normalized.length() - footer.length())
                .replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "The configured RSA " + type + " key is not valid base64", exception);
        }
    }
}
