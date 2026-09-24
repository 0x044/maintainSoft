package com.maintainsoft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class IsolatedTestProfileContextTest {

    @Test
    void contextLoadsAgainstConfiguredDisposableDatabase() {
    }
}
