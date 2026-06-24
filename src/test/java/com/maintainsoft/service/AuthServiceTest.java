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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL = "john@example.com";
    private static final String TEST_PASSWORD = "securePass123";
    private static final String TEST_NAME = "John Doe";
    private static final String TEST_PHONE = "1234567890";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final UUID DEPARTMENT_ID = UUID.randomUUID();

    private Department department;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setDeptName("Engineering");
        department.setPocName("Manager");
        department.setPocNumber(9876543210L);

        mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .authorities("ROLE_SUPERVISOR")
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterRequest createRegisterRequest() {
            return new RegisterRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_PHONE, DEPARTMENT_ID);
        }

        @Test
        @DisplayName("should register user successfully and return auth response")
        void register_Success() {
            // Arrange
            RegisterRequest request = createRegisterRequest();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(department));
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(mockUserDetails);
            when(jwtService.generateAccessToken(mockUserDetails)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn(REFRESH_TOKEN);

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.role()).isEqualTo(Role.SUPERVISOR.name());

            // Verify user was saved with correct fields
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo(TEST_NAME);
            assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedUser.getPhone()).isEqualTo(TEST_PHONE);
            assertThat(savedUser.getRole()).isEqualTo(Role.SUPERVISOR);
            assertThat(savedUser.getDepartment()).isEqualTo(department);
        }

        @Test
        @DisplayName("should throw DuplicateEmailException when email already exists")
        void register_DuplicateEmail_ThrowsException() {
            // Arrange
            RegisterRequest request = createRegisterRequest();
            User existingUser = new User();
            existingUser.setEmail(TEST_EMAIL);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("Email already registered: " + TEST_EMAIL);

            // Verify no user was saved
            verify(userRepository, never()).save(any(User.class));
            verify(departmentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when department does not exist")
        void register_DepartmentNotFound_ThrowsException() {
            // Arrange
            RegisterRequest request = createRegisterRequest();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Department not found: " + DEPARTMENT_ID);

            // Verify no user was saved
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return auth response")
        void login_Success() {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            Authentication mockAuthentication = mock(Authentication.class);
            when(mockAuthentication.getPrincipal()).thenReturn(mockUserDetails);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuthentication);

            User user = new User();
            user.setEmail(TEST_EMAIL);
            user.setRole(Role.SUPERVISOR);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(mockUserDetails)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn(REFRESH_TOKEN);

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.role()).isEqualTo(Role.SUPERVISOR.name());

            // Verify authentication was called with correct credentials
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());

            UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
            assertThat(capturedAuth.getPrincipal()).isEqualTo(TEST_EMAIL);
            assertThat(capturedAuth.getCredentials()).isEqualTo(TEST_PASSWORD);
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("should refresh tokens successfully and return auth response")
        void refresh_Success() {
            // Arrange
            String oldRefreshToken = "old-refresh-token";
            RefreshRequest request = new RefreshRequest(oldRefreshToken);

            Jwt mockJwt = Jwt.withTokenValue(oldRefreshToken)
                    .header("alg", "RS256")
                    .claim("type", "refresh")
                    .subject(TEST_EMAIL)
                    .build();

            when(jwtService.validateRefreshToken(oldRefreshToken)).thenReturn(mockJwt);
            when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(mockUserDetails);

            User user = new User();
            user.setEmail(TEST_EMAIL);
            user.setRole(Role.MANAGER);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(mockUserDetails)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn(REFRESH_TOKEN);

            // Act
            AuthResponse response = authService.refresh(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.role()).isEqualTo(Role.MANAGER.name());

            verify(jwtService).validateRefreshToken(oldRefreshToken);
            verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        }
    }
}
