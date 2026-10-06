package com.maintainsoft.service;

import com.maintainsoft.entity.RefreshToken;
import com.maintainsoft.entity.User;
import com.maintainsoft.exception.InvalidTokenException;
import com.maintainsoft.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.refresh-token.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional
    public void issue(User user, String rawToken, Jwt jwt) {
        refreshTokenRepository.save(newToken(user, rawToken, jwt, UUID.randomUUID()));
    }

    @Transactional
    public void rotate(
            User user,
            String oldRawToken,
            Jwt oldJwt,
            String newRawToken,
            Jwt newJwt
    ) {
        String oldJti = jti(oldJwt);
        RefreshToken current = refreshTokenRepository.findByJtiForUpdate(oldJti)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        validateOwner(current, user);
        if (current.getRevokedAt() != null) {
            revokeFamily(current.getFamilyId());
            throw new InvalidTokenException("Refresh token replay detected");
        }
        if (current.getExpiresAt() == null || !current.getExpiresAt().isAfter(Instant.now())) {
            current.setRevokedAt(Instant.now());
            refreshTokenRepository.save(current);
            throw new InvalidTokenException("Refresh token expired");
        }
        if (!hash(oldRawToken).equals(current.getTokenHash())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String newJti = jti(newJwt);
        current.setRevokedAt(Instant.now());
        current.setReplacedByJti(newJti);
        refreshTokenRepository.save(current);
        refreshTokenRepository.save(newToken(user, newRawToken, newJwt, current.getFamilyId()));
    }

    @Transactional
    public void revoke(String rawToken, Jwt jwt) {
        String tokenJti = jti(jwt);
        RefreshToken token = refreshTokenRepository.findByJtiForUpdate(tokenJti).orElse(null);
        if (token == null) {
            return;
        }
        validateOwner(token, token.getUser());
        if (!hash(rawToken).equals(token.getTokenHash())) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }

    /**
     * Revokes every live refresh token belonging to a user.
     *
     * <p>Called when an account is deactivated so the person cannot extend the session
     * they already hold. Access tokens are stateless and remain valid until they expire,
     * so deactivation is not instantaneous for a token already in flight.
     */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        List<RefreshToken> live = refreshTokenRepository
                .findByUser_IdAndRevokedAtIsNull(userId);
        Instant now = Instant.now();
        live.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(live);
        return live.size();
    }

    private RefreshToken newToken(User user, String rawToken, Jwt jwt, UUID familyId) {
        String tokenJti = jti(jwt);
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new InvalidTokenException("Refresh token expiry is missing");
        }
        if (!Objects.equals(user.getEmail(), jwt.getSubject())) {
            throw new InvalidTokenException("Refresh token subject does not match user");
        }

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setJti(tokenJti);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(expiresAt);
        return token;
    }

    private void validateOwner(RefreshToken token, User user) {
        if (user == null || token.getUser() == null || token.getUser().getId() == null
                || !token.getUser().getId().equals(user.getId())) {
            throw new InvalidTokenException("Refresh token does not belong to user");
        }
    }

    private void revokeFamily(UUID familyId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(familyId);
        Instant now = Instant.now();
        activeTokens.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private String jti(Jwt jwt) {
        String tokenJti = jwt == null ? null : jwt.getClaimAsString("jti");
        if (tokenJti == null || tokenJti.isBlank()) {
            throw new InvalidTokenException("Refresh token identifier is missing");
        }
        return tokenJti;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
