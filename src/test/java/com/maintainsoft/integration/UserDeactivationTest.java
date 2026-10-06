package com.maintainsoft.integration;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.RefreshToken;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.InvalidUserException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.RefreshTokenRepository;
import com.maintainsoft.repository.UserRepository;
import com.maintainsoft.service.UserManagementService;
import com.maintainsoft.testsupport.TestSecurityContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deactivating an account has to actually cut its access, and must not let a manager
 * lock the system out of its own management.
 */
@SpringBootTest
class UserDeactivationTest {

    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManager entityManager;

    private UUID departmentId;
    private UUID managerId;
    private UUID secondManagerId;
    private UUID supervisorId;
    private String managerEmail;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Department department = new Department();
            department.setDeptName("Users-" + UUID.randomUUID());
            department.setPocName("POC");
            department.setPocNumber(5_500_000L);
            departmentRepository.saveAndFlush(department);
            departmentId = department.getId();

            managerId = newUser(Role.MANAGER, "manager-" + UUID.randomUUID(), department);
            secondManagerId = newUser(Role.MANAGER, "second-" + UUID.randomUUID(), department);
            supervisorId = newUser(Role.SUPERVISOR, "sup-" + UUID.randomUUID(), department);
            managerEmail = userRepository.findById(managerId).orElseThrow().getEmail();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "delete from refresh_tokens where user_id in (:a, :b, :c)")
                    .setParameter("a", managerId)
                    .setParameter("b", secondManagerId)
                    .setParameter("c", supervisorId)
                    .executeUpdate();
            entityManager.createNativeQuery("delete from users where id in (:a, :b, :c)")
                    .setParameter("a", managerId)
                    .setParameter("b", secondManagerId)
                    .setParameter("c", supervisorId)
                    .executeUpdate();
            entityManager.createNativeQuery("delete from departments where id = :id")
                    .setParameter("id", departmentId).executeUpdate();
        });
    }

    @Test
    void deactivationArchivesTheUserAndRevokesTheirRefreshTokens() {
        issueRefreshToken(supervisorId);
        assertThat(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(supervisorId)).hasSize(1);

        deactivate(supervisorId);

        assertThat(userRepository.findById(supervisorId))
                .as("an archived user must disappear from active lookups")
                .isEmpty();
        assertThat(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(supervisorId))
                .as("a deactivated account must not be able to extend its session")
                .isEmpty();
    }

    @Test
    void aManagerCannotDeactivateThemselves() {
        Authentication caller = TestSecurityContext.asManager(managerEmail);

        assertThatThrownBy(() -> TestSecurityContext.runAs(caller,
                () -> userManagementService.deactivateUser(managerId, caller)))
                .isInstanceOf(InvalidUserException.class)
                .hasMessageContaining("your own account");
    }

    @Test
    void aManagerCanDeactivateAnotherManagerWhileOneRemains() {
        deactivate(secondManagerId);

        assertThat(userRepository.findById(secondManagerId)).isEmpty();
        assertThat(userRepository.findById(managerId))
                .as("a manager must remain available")
                .isPresent();
    }

    @Test
    void deactivatingAnUnknownUserIsNotFound() {
        Authentication caller = TestSecurityContext.asManager(managerEmail);

        assertThatThrownBy(() -> TestSecurityContext.runAs(caller,
                () -> userManagementService.deactivateUser(UUID.randomUUID(), caller)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listUsersReturnsOnlyActiveUsers() {
        deactivate(supervisorId);

        var active = TestSecurityContext.runAs(TestSecurityContext.asManager(managerEmail),
                () -> userManagementService.listUsers());

        assertThat(active.stream().map(user -> user.id()).toList())
                .contains(managerId, secondManagerId)
                .doesNotContain(supervisorId);
    }

    @Test
    void aSupervisorCannotDeactivateAnyone() {
        Authentication supervisor = TestSecurityContext.as("ROLE_SUPERVISOR");

        assertThatThrownBy(() -> TestSecurityContext.runAs(supervisor,
                () -> userManagementService.deactivateUser(supervisorId, supervisor)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private void deactivate(UUID target) {
        Authentication caller = TestSecurityContext.asManager(managerEmail);
        TestSecurityContext.runAs(caller,
                () -> userManagementService.deactivateUser(target, caller));
    }

    private UUID newUser(Role role, String prefix, Department department) {
        User user = new User();
        user.setName(prefix);
        user.setEmail(prefix + "@example.com");
        user.setPassword(passwordEncoder.encode("irrelevant-password"));
        user.setPhone("8" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        user.setRole(role);
        user.setDepartment(department);
        userRepository.saveAndFlush(user);
        return user.getId();
    }

    private void issueRefreshToken(UUID userId) {
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(userId).orElseThrow();
            RefreshToken token = new RefreshToken();
            token.setUser(user);
            token.setFamilyId(UUID.randomUUID());
            token.setJti(UUID.randomUUID().toString());
            token.setTokenHash("hash-" + UUID.randomUUID());
            token.setExpiresAt(Instant.now().plusSeconds(3600));
            refreshTokenRepository.saveAndFlush(token);
        });
    }
}