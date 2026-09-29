package com.maintainsoft.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 32) String phone,
        @NotNull UUID department
) {
    /**
     * Records generate a {@code toString()} that includes every component, which would
     * write the password to any log, error report, or debugger that prints this
     * request. Only the identifying field is safe to expose.
     */
    @Override
    public String toString() {
        return "CreateUserRequest[name=" + name + ", email=" + email
                + ", password=<redacted>, phone=" + phone + ", department=" + department + "]";
    }
}
