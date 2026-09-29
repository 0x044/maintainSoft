package com.maintainsoft.testsupport;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Publishes the embedded test database to every test class through system properties
 * before Spring builds an application context.
 *
 * <p>The extension is auto-detected (see {@code src/test/resources/junit-platform.properties}),
 * so individual tests need no {@code @DynamicPropertySource} hook and no test can
 * silently fall back to the configured application datasource.
 */
public class EmbeddedDatabaseExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        EmbeddedDatabase.start().registerAsSystemProperties();
    }
}
