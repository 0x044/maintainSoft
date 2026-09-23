package com.maintainsoft.service;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return buildAuthResponse(userDetails, user.getRole());
    }

    public AuthResponse refresh(RefreshRequest request) {
        Jwt jwt = jwtService.validateRefreshToken(request.refreshToken());
        String email = jwt.getSubject();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        return buildAuthResponse(userDetails, user.getRole());
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails, Role role) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(accessToken, refreshToken, userDetails.getUsername(), role.name());
    }
}
