package com.maintainsoft.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/v1")
public class HealthController {
    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health(){
        Map<String, String> res = new LinkedHashMap<>();
        return ResponseEntity.accepted().body(res);
    }
}
