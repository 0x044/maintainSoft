package com.maintainsoft.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    /**
     * Records generate a {@code toString()} that includes every component, which would
     * write the password to any log, error report, or debugger that prints this
     * request. Only the identifying field is safe to expose.
     */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=<redacted>]";
    }
}
