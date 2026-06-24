package com.maintainsoft.service;

import com.maintainsoft.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private JwtService jwtService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TOKEN_VALUE = "encoded-jwt-token-value";

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername(TEST_EMAIL)
                .password("password")
                .authorities("ROLE_SUPERVISOR")
                .build();
    }

    private Jwt createMockEncodedJwt() {
        return Jwt.withTokenValue(TOKEN_VALUE)
                .header("alg", "RS256")
                .claim("type", "access")
                .subject(TEST_EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("should generate access token with correct claims")
        void generateAccessToken_Success() throws MalformedURLException {
            // Arrange
            Jwt mockJwt = createMockEncodedJwt();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            String token = jwtService.generateAccessToken(userDetails);

            // Assert
            assertThat(token).isEqualTo(TOKEN_VALUE);

            // Verify the claims passed to the encoder
            ArgumentCaptor<JwtEncoderParameters> paramsCaptor =
                    ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(paramsCaptor.capture());

            JwtClaimsSet claims = paramsCaptor.getValue().getClaims();
            assertThat(claims.getSubject()).isEqualTo(TEST_EMAIL);
            assertThat(claims.getClaimAsString("iss")).isEqualTo("https://msi.leo-blenny.ts.net");
            assertThat(claims.<String>getClaim("type")).isEqualTo("access");
            assertThat(claims.<String>getClaim("scope")).isEqualTo("ROLE_SUPERVISOR");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiresAt()).isNotNull();
            assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("should include all authorities in scope claim")
        void generateAccessToken_MultipleAuthorities() {
            // Arrange
            UserDetails multiAuthUser = User.withUsername(TEST_EMAIL)
                    .password("password")
                    .authorities("ROLE_SUPERVISOR", "ROLE_MANAGER")
                    .build();

            Jwt mockJwt = createMockEncodedJwt();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            jwtService.generateAccessToken(multiAuthUser);

            // Assert
            ArgumentCaptor<JwtEncoderParameters> paramsCaptor =
                    ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(paramsCaptor.capture());

            JwtClaimsSet claims = paramsCaptor.getValue().getClaims();
            String scope = claims.getClaim("scope");
            assertThat(scope).contains("ROLE_SUPERVISOR");
            assertThat(scope).contains("ROLE_MANAGER");
        }

        @Test
        @DisplayName("should set expiration 15 minutes after issuedAt")
        void generateAccessToken_ExpirationTime() {
            // Arrange
            Jwt mockJwt = createMockEncodedJwt();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            jwtService.generateAccessToken(userDetails);

            // Assert
            ArgumentCaptor<JwtEncoderParameters> paramsCaptor =
                    ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(paramsCaptor.capture());

            JwtClaimsSet claims = paramsCaptor.getValue().getClaims();
            long differenceSeconds = claims.getExpiresAt().getEpochSecond()
                    - claims.getIssuedAt().getEpochSecond();
            assertThat(differenceSeconds).isEqualTo(15 * 60); // 15 minutes in seconds
        }
    }

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("should generate refresh token with correct claims")
        void generateRefreshToken_Success() {
            // Arrange
            Jwt mockJwt = Jwt.withTokenValue(TOKEN_VALUE)
                    .header("alg", "RS256")
                    .claim("type", "refresh")
                    .subject(TEST_EMAIL)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            String token = jwtService.generateRefreshToken(userDetails);

            // Assert
            assertThat(token).isEqualTo(TOKEN_VALUE);

            ArgumentCaptor<JwtEncoderParameters> paramsCaptor =
                    ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(paramsCaptor.capture());

            JwtClaimsSet claims = paramsCaptor.getValue().getClaims();
            assertThat(claims.getSubject()).isEqualTo(TEST_EMAIL);
            assertThat(claims.getIssuer().toString()).isEqualTo("https://msi.leo-blenny.ts.net");
            assertThat(claims.<String>getClaim("type")).isEqualTo("refresh");
            // Refresh token should NOT contain scope
            assertThat(claims.<String>getClaim("scope")).isNull();
        }

        @Test
        @DisplayName("should set expiration 7 days after issuedAt")
        void generateRefreshToken_ExpirationTime() {
            // Arrange
            Jwt mockJwt = createMockEncodedJwt();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            jwtService.generateRefreshToken(userDetails);

            // Assert
            ArgumentCaptor<JwtEncoderParameters> paramsCaptor =
                    ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(paramsCaptor.capture());

            JwtClaimsSet claims = paramsCaptor.getValue().getClaims();
            long differenceSeconds = claims.getExpiresAt().getEpochSecond()
                    - claims.getIssuedAt().getEpochSecond();
            assertThat(differenceSeconds).isEqualTo(7 * 24 * 60 * 60); // 7 days in seconds
        }
    }

    @Nested
    @DisplayName("validateRefreshToken()")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("should return Jwt when refresh token is valid")
        void validateRefreshToken_Valid() {
            // Arrange
            String refreshToken = "valid-refresh-token";
            Jwt expectedJwt = Jwt.withTokenValue(refreshToken)
                    .header("alg", "RS256")
                    .claim("type", "refresh")
                    .subject(TEST_EMAIL)
                    .build();

            when(jwtDecoder.decode(refreshToken)).thenReturn(expectedJwt);

            // Act
            Jwt result = jwtService.validateRefreshToken(refreshToken);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSubject()).isEqualTo(TEST_EMAIL);
            assertThat(result.getClaimAsString("type")).isEqualTo("refresh");
            verify(jwtDecoder).decode(refreshToken);
        }

        @Test
        @DisplayName("should throw InvalidTokenException when token type is not 'refresh'")
        void validateRefreshToken_NonRefreshType_ThrowsException() {
            // Arrange
            String accessToken = "access-token-pretending-to-be-refresh";
            Jwt accessJwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "RS256")
                    .claim("type", "access")
                    .subject(TEST_EMAIL)
                    .build();

            when(jwtDecoder.decode(accessToken)).thenReturn(accessJwt);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("should throw InvalidTokenException when token type claim is null")
        void validateRefreshToken_NullType_ThrowsException() {
            // Arrange
            String tokenWithNoType = "token-without-type";
            Jwt jwtNoType = Jwt.withTokenValue(tokenWithNoType)
                    .header("alg", "RS256")
                    .subject(TEST_EMAIL)
                    .claim("other", "value")
                    .build();

            when(jwtDecoder.decode(tokenWithNoType)).thenReturn(jwtNoType);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateRefreshToken(tokenWithNoType))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("should throw InvalidTokenException when JwtDecoder throws JwtException")
        void validateRefreshToken_JwtException_ThrowsInvalidTokenException() {
            // Arrange
            String invalidToken = "corrupted-token";
            when(jwtDecoder.decode(invalidToken)).thenThrow(new JwtException("Token expired or malformed"));

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateRefreshToken(invalidToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("should throw InvalidTokenException when JwtDecoder throws BadJwtException")
        void validateRefreshToken_BadJwtException_ThrowsInvalidTokenException() {
            // Arrange
            String badToken = "bad-jwt-token";
            when(jwtDecoder.decode(badToken)).thenThrow(new BadJwtException("Bad JWT"));

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateRefreshToken(badToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid username or password");
        }
    }
}
