package com.maintainsoft.service;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.RegisterRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    AuthResponse register(RegisterRequest registerRequest){
        if(userRepository.findByEmail(registerRequest.email()).isEmpty()){
            User user = new User();
            user.setName(registerRequest.name());
            user.setEmail(registerRequest.email());
            user.setPassword(passwordEncoder.encode(registerRequest.password()));
            user.setPhone(registerRequest.phone());
            user.setRole(Role.SUPERVISOR);
            user.setDepartment(registerRequest.department());
            userRepository.save(user);
        }

        AuthResponse response = new AuthResponse();
    }
}
