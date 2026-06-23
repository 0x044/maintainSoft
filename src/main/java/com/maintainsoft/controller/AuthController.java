package com.maintainsoft.controller;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.dto.RegisterRequest;
import com.maintainsoft.exception.DuplicateEmailException;
import com.maintainsoft.security.UserDetailServiceImpl;
import com.maintainsoft.service.AuthService;
import com.maintainsoft.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    public AuthService authService;
    public JwtService jwtService;
    private UserDetailServiceImpl userDetailService;

    @GetMapping("/token")
    ResponseEntity<String> token(@RequestBody LoginRequest loginRequest) {
        UserDetails userDetails = userDetailService.loadUserByUsername(loginRequest.email());
        String token = jwtService.generateAccessToken(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(RegisterRequest registerRequest) {

    }

    @PostMapping("/login")
    AuthResponse login(LoginRequest loginRequest) {

    }

    @PostMapping("/refresh")
    AuthResponse refresh(RefreshRequest refreshRequest) {

    }
}
