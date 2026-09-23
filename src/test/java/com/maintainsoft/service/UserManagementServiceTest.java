package com.maintainsoft.service;

import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.DuplicateEmailException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private final UUID departmentId = UUID.randomUUID();
    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(departmentId);
        department.setDeptName("Engineering");
    }

    @Test
    void createsSupervisorWithEncodedPassword() {
        CreateUserRequest request = request();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userManagementService.createSupervisor(request);

        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(response.departmentId()).isEqualTo(departmentId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.SUPERVISOR);
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    void rejectsDuplicateEmail() {
        CreateUserRequest request = request();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userManagementService.createSupervisor(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsUnknownDepartment() {
        CreateUserRequest request = request();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.createSupervisor(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(departmentId.toString());

        verify(userRepository, never()).save(any(User.class));
    }

    private CreateUserRequest request() {
        return new CreateUserRequest(
                "Jane Doe",
                "jane@example.com",
                "password123",
                "1234567890",
                departmentId
        );
    }
}
