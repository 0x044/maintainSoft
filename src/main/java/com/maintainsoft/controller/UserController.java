package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
