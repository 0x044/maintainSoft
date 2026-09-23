package com.maintainsoft.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validLoginRequestPassesValidation() {
        Set<?> violations = validator.validate(new LoginRequest("user@example.com", "password"));

        assertThat(violations).isEmpty();
    }

    @Test
    void loginRequestRejectsBlankAndMalformedValues() {
        Set<?> violations = validator.validate(new LoginRequest("not-an-email", " "));

        assertThat(violations).hasSize(2);
    }

    @Test
    void refreshRequestRejectsBlankToken() {
        Set<?> violations = validator.validate(new RefreshRequest(" "));

        assertThat(violations).hasSize(1);
    }

    @Test
    void departmentRequestRejectsBlankNamesAndNonPositiveNumber() {
        Set<?> violations = validator.validate(new DepartmentRequest(" ", "", 0L));

        assertThat(violations).hasSize(3);
    }

    @Test
    void validRegisterRequestPassesValidation() {
        RegisterRequest request = new RegisterRequest(
                "User", "user@example.com", "password", "1234567890", UUID.randomUUID()
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
