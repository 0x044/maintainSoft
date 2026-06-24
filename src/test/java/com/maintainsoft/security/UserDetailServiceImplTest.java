package com.maintainsoft.security;

import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailServiceImpl Unit Tests")
class UserDetailServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailServiceImpl userDetailService;

    private User buildUser(String email, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setName("Test User");
        user.setPhone("9876543210");
        return user;
    }

    @Nested
    @DisplayName("When user exists")
    class WhenUserExists {

        @Test
        @DisplayName("Should return UserDetails with correct email as username")
        void loadUserByUsername_returnsUserDetailsWithCorrectEmail() {
            // Arrange
            String email = "balu@softex.com";
            User user = buildUser(email, "encoded-password", Role.MANAGER);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails.getUsername()).isEqualTo(email);
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should return UserDetails with correct password")
        void loadUserByUsername_returnsUserDetailsWithCorrectPassword() {
            // Arrange
            String email = "balu@softex.com";
            User user = buildUser(email, "bcrypt-hashed-pass", Role.MANAGER);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails.getPassword()).isEqualTo("bcrypt-hashed-pass");
        }

        @Test
        @DisplayName("Should return UserDetails with MANAGER role")
        void loadUserByUsername_returnsUserDetailsWithManagerRole() {
            // Arrange
            String email = "manager@softex.com";
            User user = buildUser(email, "password", Role.MANAGER);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails.getAuthorities())
                    .extracting(auth -> auth.getAuthority())
                    .containsExactly("ROLE_MANAGER");
        }

        @Test
        @DisplayName("Should return UserDetails with SUPERVISOR role")
        void loadUserByUsername_returnsUserDetailsWithSupervisorRole() {
            // Arrange
            String email = "supervisor@softex.com";
            User user = buildUser(email, "password", Role.SUPERVISOR);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails.getAuthorities())
                    .extracting(auth -> auth.getAuthority())
                    .containsExactly("ROLE_SUPERVISOR");
        }

        @Test
        @DisplayName("Should return fully populated UserDetails with all fields correct")
        void loadUserByUsername_returnsFullyPopulatedUserDetails() {
            // Arrange
            String email = "complete@softex.com";
            User user = buildUser(email, "secret123", Role.MANAGER);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserDetails userDetails = userDetailService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails.getUsername()).isEqualTo("complete@softex.com");
            assertThat(userDetails.getPassword()).isEqualTo("secret123");
            assertThat(userDetails.getAuthorities())
                    .extracting(auth -> auth.getAuthority())
                    .containsExactly("ROLE_MANAGER");
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("When user does not exist")
    class WhenUserDoesNotExist {

        @Test
        @DisplayName("Should throw UsernameNotFoundException with descriptive message")
        void loadUserByUsername_throwsUsernameNotFoundException() {
            // Arrange
            String email = "nonexistent@softex.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: " + email);
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException for empty email")
        void loadUserByUsername_throwsExceptionForEmptyEmail() {
            // Arrange
            String email = "";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found: ");
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should call repository exactly once")
        void loadUserByUsername_callsRepositoryOnce() {
            // Arrange
            String email = "missing@softex.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class);
            verify(userRepository, times(1)).findByEmail(email);
            verifyNoMoreInteractions(userRepository);
        }
    }
}
