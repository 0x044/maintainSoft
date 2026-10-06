package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    @PostMapping
    ResponseEntity<UserResponse> createSupervisor(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userManagementService.createSupervisor(request));
    }

    @GetMapping
    ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    /**
     * Archives a user and revokes their refresh tokens. Archive rather than delete, so
     * historical records keep a valid author.
     */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivateUser(
            @PathVariable UUID id,
            Authentication authentication) {
        userManagementService.deactivateUser(id, authentication);
        return ResponseEntity.noContent().build();
    }
}