package com.maintainsoft.service;

import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.DuplicateEmailException;
import com.maintainsoft.exception.InvalidUserException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public UserResponse createSupervisor(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        }

        Department department = departmentRepository.findById(request.department())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + request.department()
                ));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.SUPERVISOR);
        user.setDepartment(department);

        User savedUser = userRepository.save(user);
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole(),
                savedUser.getDepartment().getId()
        );
    }

    /**
     * Archives a user and cuts their ability to extend an existing session.
     *
     * <p>The record is archived rather than deleted, so historical repairs keep a valid
     * author, and every live refresh token is revoked so the account cannot mint a new
     * session. An access token already issued is stateless and stays usable until it
     * expires; deactivation is not instantaneous for a token in flight.
     */
    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public UserResponse deactivateUser(UUID id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        String caller = callerEmail(authentication);
        if (user.getEmail().equals(caller)) {
            throw new InvalidUserException("You cannot deactivate your own account");
        }
        if (user.getRole() == Role.MANAGER && countActiveManagers() <= 1) {
            throw new InvalidUserException(
                    "Cannot deactivate the last remaining manager");
        }

        user.setDeleted(true);
        User saved = userRepository.save(user);
        refreshTokenService.revokeAllForUser(id);

        return new UserResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole(),
                saved.getDepartment() == null ? null : saved.getDepartment().getId()
        );
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole(),
                        user.getDepartment() == null ? null : user.getDepartment().getId()
                ))
                .toList();
    }

    private long countActiveManagers() {
        return userRepository.findAllByRoleAndDeletedFalse(Role.MANAGER).size();
    }

    private String callerEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new InvalidUserException("Authentication is required to deactivate a user");
        }
        return authentication.getName();
    }
}
