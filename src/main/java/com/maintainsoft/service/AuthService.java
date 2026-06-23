package com.maintainsoft.service;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.dto.RegisterRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.DuplicateEmailException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        }

        Department department = departmentRepository.findById(request.department())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.department()));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.SUPERVISOR);
        user.setDepartment(department);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return buildAuthResponse(userDetails, user.getRole());
    }

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
