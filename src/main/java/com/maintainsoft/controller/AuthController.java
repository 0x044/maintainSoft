package com.maintainsoft.controller;

import com.maintainsoft.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    public UserRepository userRepository;

    @GetMapping("/hello")
    ResponseEntity<String> hello(){
        return ResponseEntity.ok().body("hello!");
    }
}
