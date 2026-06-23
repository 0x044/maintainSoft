package com.maintainsoft.service;

import com.maintainsoft.exception.InvalidTokenException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateAccessToken(UserDetails userDetails) {
        String scope = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(userDetails.getUsername())
                .claim("scope", scope)
                .claim("type", "access")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .subject(userDetails.getUsername())
                .claim("type", "refresh")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }

    public Jwt validateRefreshToken(String refreshToken) {
        try{
            Jwt jwt = jwtDecoder.decode(refreshToken);
            String type = jwt.getClaimAsString("type");

            if(!"refresh".equals(type)){
                throw new InvalidTokenException("Invalid username or password");
            }

            return jwt;
        }catch (JwtException e){
            throw new InvalidTokenException("Invalid username or password");
        }
    }
}
