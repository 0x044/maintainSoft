package com.maintainsoft.service;

import com.maintainsoft.dto.AuthResponse;
import com.maintainsoft.dto.LoginRequest;
import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
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
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

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
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .authorities("ROLE_SUPERVISOR")
                .build();
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
