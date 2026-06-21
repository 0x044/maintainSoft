package com.maintainsoft.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    void generateAccessToken(UserDetails userDetails){

    }

    void generateRefreshToken(UserDetails userDetails){

    }

    void validateRefreshToken(String refreshToken){

    }
}
