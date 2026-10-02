package com.maintainsoft.security;

import com.maintainsoft.MaintainsoftApplication;
import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.service.DepartmentService;
import com.maintainsoft.service.MachineStatusService;
import com.maintainsoft.service.UserManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Method security is the second line of defense behind the URL rules.
 *
 * <p>{@code SecurityConfig} can only describe routes that already exist, so a new
 * controller, a scheduled task, or an internal call would bypass the route rules
 * entirely. Annotating the service layer means the rule travels with the operation
 * itself.
 */
@SpringBootTest(classes = MaintainsoftApplication.class)
class MethodSecurityTest {

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private MachineStatusService machineStatusService;
    @Autowired
    private UserManagementService userManagementService;

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void servicesAreProxiedSoAnnotationsAreEnforced() {
        assertThat(AopUtils.isAopProxy(departmentService))
                .as("service must be proxied for @PreAuthorize to be enforced")
                .isTrue();
    }

    @Test
    void supervisorCannotCreateDepartmentsThroughTheServiceLayer() {
        authenticate("SUPERVISOR");

        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentRequest("Dept", "POC", 123L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void supervisorCannotArchiveDepartmentsThroughTheServiceLayer() {
        authenticate("SUPERVISOR");

        assertThatThrownBy(() -> departmentService.deleteDepartment(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void supervisorCannotCreateSupervisorsThroughTheServiceLayer() {
        authenticate("SUPERVISOR");

        assertThatThrownBy(() -> userManagementService.createSupervisor(
                new CreateUserRequest("Jane", "jane@example.com", "password123",
                        "1234567890", UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void supervisorCannotCreateCustomMachineStatusesThroughTheServiceLayer() {
        authenticate("SUPERVISOR");

        assertThatThrownBy(() -> machineStatusService.createCustomStatus(
                new CreateMachineStatusRequest("Special", "#123456")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anonymousCallerCannotCreateDepartments() {
        // With no Authentication at all the expression cannot be evaluated, so Spring
        // reports missing credentials rather than denied access. Both are refusals, and
        // neither reaches the business logic.
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentRequest("Dept", "POC", 123L)))
                .isInstanceOf(AuthenticationException.class);
    }

    private void authenticate(String role) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "caller@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}