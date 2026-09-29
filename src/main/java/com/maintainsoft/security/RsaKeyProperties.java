package com.maintainsoft.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Location of the RSA key pair used to sign and verify JSON Web Tokens.
 *
 * <p>The locations are Spring resource references, so a deployment can point them at
 * a mounted secret ({@code file:/run/secrets/jwt-private-key.pem}) instead of
 * bundling key material in the application JAR.
 */
@ConfigurationProperties(prefix = "app.security.rsa")
public record RsaKeyProperties(String privateKey, String publicKey) {
}
