package com.maintainsoft.testsupport;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Supplies the test suite with a throwaway JWT signing key pair.
 *
 * <p>The key pair is generated once per test JVM and published through system
 * properties, which take precedence over {@code application.properties}. That keeps the
 * suite independent of the developer's signing keys while still using the real
 * datasource from configuration.
 *
 * <p>The database is deliberately <em>not</em> replaced here: the suite runs against the
 * same PostgreSQL instance the application uses.
 */
public class TestInfrastructureExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        EphemeralRsaKeys.registerAsSystemProperties();
    }

    /**
     * A key pair generated once per test JVM and written to a temporary directory.
     */
    static final class EphemeralRsaKeys {

        private static final Object LOCK = new Object();

        private static Path directory;

        private EphemeralRsaKeys() {
        }

        static void registerAsSystemProperties() {
            synchronized (LOCK) {
                if (directory == null) {
                    directory = generate();
                }
            }
            System.setProperty("app.security.rsa.private-key",
                    "file:" + directory.resolve("private.key"));
            System.setProperty("app.security.rsa.public-key",
                    "file:" + directory.resolve("public.key"));
        }

        private static Path generate() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();

                Path directory = Files.createTempDirectory("maintainsoft-test-keys");
                directory.toFile().deleteOnExit();
                write(directory.resolve("private.key"), "PRIVATE KEY",
                        keyPair.getPrivate().getEncoded());
                write(directory.resolve("public.key"), "PUBLIC KEY",
                        keyPair.getPublic().getEncoded());
                return directory;
            } catch (IOException | NoSuchAlgorithmException exception) {
                throw new IllegalStateException(
                        "Unable to create the throwaway RSA key pair used by the test suite", exception);
            }
        }

        private static void write(Path path, String label, byte[] encoded) throws IOException {
            String pem = "-----BEGIN " + label + "-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                    + "\n-----END " + label + "-----\n";
            Files.writeString(path, pem, StandardCharsets.UTF_8);
            path.toFile().deleteOnExit();
        }
    }
}
