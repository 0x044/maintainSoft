package com.maintainsoft.controller;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.dto.RegisterRequest;
import com.maintainsoft.repository.UserRepository;
import com.maintainsoft.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    public AuthService authService;

    @GetMapping("/hello")
    ResponseEntity<String> hello() {
        return ResponseEntity.ok().body("hello!");
    }

    @PostMapping("/register")
    AuthResponse register(RegisterRequest registerRequest){

    }

    @PostMapping("/login")
    AuthResponse login(LoginRequest loginRequest){

    }

    @PostMapping("/refresh")
    AuthResponse refresh(RefreshRequest refreshRequest){

    }
}
