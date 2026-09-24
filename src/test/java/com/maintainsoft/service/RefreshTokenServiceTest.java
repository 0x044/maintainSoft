package com.maintainsoft.service;

import com.maintainsoft.entity.RefreshToken;
import com.maintainsoft.entity.User;
import com.maintainsoft.exception.InvalidTokenException;
import com.maintainsoft.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void storesOnlyAHashWhenIssuingToken() {
        User user = user();
        Jwt jwt = jwt("jti-1", user.getEmail());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.issue(user, "raw-refresh-token", jwt);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo("raw-refresh-token");
        assertThat(stored.getJti()).isEqualTo("jti-1");
        assertThat(stored.getUser()).isSameAs(user);
    }

    @Test
    void rotatesAnActiveTokenWithinTheSameFamily() {
        User user = user();
        RefreshToken current = token(user, "jti-old", "old-raw", UUID.randomUUID());
        Jwt oldJwt = jwt("jti-old", user.getEmail());
        Jwt newJwt = jwt("jti-new", user.getEmail());
        when(refreshTokenRepository.findByJtiForUpdate("jti-old")).thenReturn(java.util.Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.rotate(user, "old-raw", oldJwt, "new-raw", newJwt);

        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(current.getReplacedByJti()).isEqualTo("jti-new");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        RefreshToken replacement = captor.getAllValues().get(1);
        assertThat(replacement.getFamilyId()).isEqualTo(current.getFamilyId());
        assertThat(replacement.getJti()).isEqualTo("jti-new");
    }

    @Test
    void rejectsReplayAndRevokesRemainingFamilyTokens() {
        User user = user();
        RefreshToken current = token(user, "jti-old", "old-raw", UUID.randomUUID());
        current.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByJtiForUpdate("jti-old")).thenReturn(java.util.Optional.of(current));
        when(refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(current.getFamilyId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> refreshTokenService.rotate(
                user,
                "old-raw",
                jwt("jti-old", user.getEmail()),
                "new-raw",
                jwt("jti-new", user.getEmail())
        )).isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("replay");

        verify(refreshTokenRepository).findByFamilyIdAndRevokedAtIsNull(current.getFamilyId());
    }

    @Test
    void rejectsAStoredTokenWhenTheRawValueDoesNotMatch() {
        User user = user();
        RefreshToken current = token(user, "jti-old", "old-raw", UUID.randomUUID());
        when(refreshTokenRepository.findByJtiForUpdate("jti-old")).thenReturn(java.util.Optional.of(current));

        assertThatThrownBy(() -> refreshTokenService.rotate(
                user,
                "tampered-raw",
                jwt("jti-old", user.getEmail()),
                "new-raw",
                jwt("jti-new", user.getEmail())
        )).isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokesAnActiveToken() {
        User user = user();
        RefreshToken current = token(user, "jti-old", "old-raw", UUID.randomUUID());
        when(refreshTokenRepository.findByJtiForUpdate("jti-old")).thenReturn(java.util.Optional.of(current));
        when(refreshTokenRepository.save(current)).thenReturn(current);

        refreshTokenService.revoke("old-raw", jwt("jti-old", user.getEmail()));

        assertThat(current.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(current);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        return user;
    }

    private Jwt jwt(String jti, String subject) {
        return Jwt.withTokenValue("token-" + jti)
                .header("alg", "RS256")
                .subject(subject)
                .claim("type", "refresh")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private RefreshToken token(User user, String jti, String rawToken, UUID familyId) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setJti(jti);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }

    private String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
