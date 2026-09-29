package com.maintainsoft.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Request records are printed by logs, error reports, and debuggers. A generated
 * {@code toString()} would include every component, so the password fields must be
 * redacted explicitly.
 */
class SecretBearingRequestRedactionTest {

    private static final String PASSWORD = "sup3r-s3cret-value";

    @Test
    void loginRequestNeverPrintsThePassword() {
        String printed = new LoginRequest("user@example.com", PASSWORD).toString();

        assertThat(printed).doesNotContain(PASSWORD);
        assertThat(printed).contains("user@example.com");
    }

    @Test
    void createUserRequestNeverPrintsThePassword() {
        java.util.UUID department = java.util.UUID.randomUUID();

        String printed = new CreateUserRequest(
                "Jane Doe", "jane@example.com", PASSWORD, "1234567890", department).toString();

        assertThat(printed).doesNotContain(PASSWORD);
        assertThat(printed).contains("jane@example.com");
    }
}
