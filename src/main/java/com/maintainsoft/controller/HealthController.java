package com.maintainsoft.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "user", auth.getName()
        ));
    }
}
