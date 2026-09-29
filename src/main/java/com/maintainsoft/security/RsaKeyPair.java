package com.maintainsoft.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * The RSA key pair used to sign and verify JSON Web Tokens.
 */
public record RsaKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
}
