package com.maintainsoft.integration;

import com.maintainsoft.dto.RefreshRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import com.maintainsoft.security.RsaKeyPair;
import com.maintainsoft.service.AuthService;
import com.maintainsoft.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A refresh token outlives the account that owns it.
 *
 * <p>When the user is deleted while their token is still valid, token validation
 * succeeds and the failure only surfaces when the account is loaded. That must remain
 * an authentication failure. A sliced test can hide this: without
 * {@code GlobalExceptionHandler} on the classpath Spring Security answers 401 for
 * any {@code AuthenticationException}, so the defect is invisible until the real
 * application registers the advice and its catch-all {@code Exception} handler turns
 * the failure into a 500.
 */
@SpringBootTest
@Transactional
class DeletedUserAuthenticationContractTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RsaKeyPair rsaKeyPair;

    private String email;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        Department department = new Department();
        department.setDeptName("Departed-" + UUID.randomUUID());
        department.setPocName("POC");
        department.setPocNumber(7_700_000L);
        departmentRepository.saveAndFlush(department);

        email = "departed-" + UUID.randomUUID() + "@example.com";

        User user = new User();
        user.setName("Departed");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("irrelevant-password"));
        user.setPhone("9" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        user.setRole(Role.SUPERVISOR);
        user.setDepartment(department);
        userRepository.saveAndFlush(user);

        validRefreshToken = signedRefreshToken(email);
    }

    @Test
    void refreshingAfterTheAccountIsDeletedIsAnAuthenticationFailure() {
        deleteUser();

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(validRefreshToken)))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    /**
     * Deletes the row the way the entity model does, then evicts the cached
     * persistence context state so the next load genuinely misses.
     */
    private void deleteUser() {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setDeleted(true);
        userRepository.saveAndFlush(user);
        userRepository.flush();
    }

    private String signedRefreshToken(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtService.JWT_ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(subject)
                .claim("type", "refresh")
                .claim("jti", UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
