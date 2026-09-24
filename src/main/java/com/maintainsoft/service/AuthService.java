package com.maintainsoft.service;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.LogoutRequest;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return issueNewTokenPair(userDetails, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Jwt oldJwt = jwtService.validateRefreshToken(request.refreshToken());
        String email = oldJwt.getSubject();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        String accessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        Jwt newJwt = jwtService.validateRefreshToken(newRefreshToken);
        refreshTokenService.rotate(
                user,
                request.refreshToken(),
                oldJwt,
                newRefreshToken,
                newJwt
        );
        return new AuthResponse(accessToken, newRefreshToken, userDetails.getUsername(), user.getRole().name());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        Jwt jwt = jwtService.validateRefreshToken(request.refreshToken());
        refreshTokenService.revoke(request.refreshToken(), jwt);
    }

    private AuthResponse issueNewTokenPair(UserDetails userDetails, User user) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Jwt refreshJwt = jwtService.validateRefreshToken(refreshToken);
        refreshTokenService.issue(user, refreshToken, refreshJwt);
        return new AuthResponse(accessToken, refreshToken, userDetails.getUsername(), user.getRole().name());
    }
}
