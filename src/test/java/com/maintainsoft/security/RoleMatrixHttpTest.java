package com.maintainsoft.security;

import com.maintainsoft.controller.DepartmentController;
import com.maintainsoft.controller.HealthController;
import com.maintainsoft.controller.MachineController;
import com.maintainsoft.controller.MachineStatusController;
import com.maintainsoft.controller.RepairController;
import com.maintainsoft.controller.SpareController;
import com.maintainsoft.controller.UserController;
import com.maintainsoft.service.MachineService;
import com.maintainsoft.service.MachineStatusService;
import com.maintainsoft.service.RepairService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Executes the complete role matrix.
 *
 * <p>The URL rules in {@link SecurityConfig} are the only authorization in front of
 * department, machine, spare, and user operations; the service layer for those does
 * not check the caller. A single missed matcher would therefore be an authorization
 * bypass, so every method/role combination is asserted explicitly.
 */
@SpringBootTest(classes = RoleMatrixHttpTest.TestApplication.class)
@AutoConfigureMockMvc
class RoleMatrixHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserManagementService userManagementService;
    @MockitoBean private MachineService machineService;
    @MockitoBean private MachineStatusService machineStatusService;
    @MockitoBean private SpareService spareService;
    @MockitoBean private RepairService repairService;
    @MockitoBean private com.maintainsoft.service.DepartmentService departmentService;

    private static final UUID ID = UUID.randomUUID();

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
        // A user with no maintenance role must never be able to mutate anything.
        "POST,   /api/v1/machines,                        REPORTER,  403",
        "PATCH,  /api/v1/machines/{id},                   REPORTER,  403",
        "DELETE, /api/v1/machines/{id},                   REPORTER,  403",
        "POST,   /api/v1/spares,                          REPORTER,  403",
        "PATCH,  /api/v1/spares/{id},                     REPORTER,  403",
        "DELETE, /api/v1/spares/{id},                     REPORTER,  403",
        "POST,   /api/v1/spares/{id}/stock/receive,       REPORTER,  403",
        "POST,   /api/v1/spares/{id}/stock/issue,         REPORTER,  403",
        "POST,   /api/v1/spares/{id}/stock/adjust,        REPORTER,  403",
        "POST,   /api/v1/machine-statuses,                REPORTER,  403",
        "POST,   /api/v1/users,                           REPORTER,  403",
        "POST,   /api/v1/departments,                     REPORTER,  403",
        "PATCH,  /api/v1/departments/{id},                REPORTER,  403",
        "DELETE, /api/v1/departments/{id},                REPORTER,  403",
        "POST,   /api/v1/repairs,                         REPORTER,  403",
        "PATCH,  /api/v1/repairs/{id},                    REPORTER,  403",
        "POST,   /api/v1/repairs/{id}/claim,              REPORTER,  403",
        "POST,   /api/v1/repairs/{id}/updates,            REPORTER,  403",
        "POST,   /api/v1/repairs/{id}/costs,              REPORTER,  403",
    })
    void reporterCannotMutate(String method, String path, String role, int expected) throws Exception {
        assertStatus(method, path, role, expected);
    }

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
        // Supervisors manage machines, spares, stock and repairs, but not master data
        // or other users.
        "POST,   /api/v1/machine-statuses,                SUPERVISOR, 403",
        "POST,   /api/v1/users,                           SUPERVISOR, 403",
        "POST,   /api/v1/departments,                     SUPERVISOR, 403",
        "PATCH,  /api/v1/departments/{id},                SUPERVISOR, 403",
        "DELETE, /api/v1/departments/{id},                SUPERVISOR, 403",
        "POST,   /api/v1/repairs/{id}/claim,              SUPERVISOR, 200",
        "POST,   /api/v1/repairs/{id}/updates,            SUPERVISOR, 201",
        "POST,   /api/v1/repairs/{id}/costs,              SUPERVISOR, 201",
        "PATCH,  /api/v1/repairs/{id}/assignment,         SUPERVISOR, 403",
        "PATCH,  /api/v1/repairs/{id},                    SUPERVISOR, 200",
    })
    void supervisorBoundaries(String method, String path, String role, int expected) throws Exception {
        assertStatus(method, path, role, expected);
    }

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
        "POST,   /api/v1/machine-statuses,                MANAGER,   201",
        "POST,   /api/v1/users,                           MANAGER,   201",
        "POST,   /api/v1/departments,                     MANAGER,   201",
        "POST,   /api/v1/repairs,                         MANAGER,   201",
        "POST,   /api/v1/repairs/{id}/claim,              MANAGER,   403",
        "PATCH,  /api/v1/repairs/{id}/assignment,         MANAGER,   200",
        "PATCH,  /api/v1/departments/{id},                MANAGER,   200",
        "DELETE, /api/v1/departments/{id},                MANAGER,   204",
    })
    void managerBoundaries(String method, String path, String role, int expected) throws Exception {
        assertStatus(method, path, role, expected);
    }

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
        "GET,    /api/v1/departments,                     REPORTER,  200",
        "GET,    /api/v1/machines,                        REPORTER,  200",
        "GET,    /api/v1/spares,                          REPORTER,  200",
        "GET,    /api/v1/machine-statuses,                REPORTER,  200",
        "GET,    /api/v1/repairs,                         REPORTER,  200",
        "GET,    /api/v1/health,                          REPORTER,  200",
    })
    void anyAuthenticatedUserMayRead(String method, String path, String role, int expected) throws Exception {
        assertStatus(method, path, role, expected);
    }

    @ParameterizedTest(name = "unauthenticated {0} {1} -> {2}")
    @CsvSource({
        "GET,    /api/v1/machines,   401",
        "GET,    /api/v1/spares,     401",
        "GET,    /api/v1/repairs,    401",
        "POST,   /api/v1/machines,   401",
        "DELETE, /api/v1/machines/{id}, 401",
        "POST,   /api/v1/users,      401",
    })
    void anonymousIsRejected(String method, String path, int expected) throws Exception {
        assertStatus(method, path, null, expected);
    }

    private void assertStatus(String method, String path, String role, int expected) throws Exception {
        String resolved = path.replace("{id}", ID.toString());
        // A body that satisfies bean validation, so a 400 can only come from the
        // request contract and never masks the authorization status under test.
        MockHttpServletRequestBuilder request = switch (method) {
            case "GET" -> get(resolved);
            case "POST" -> post(resolved).contentType("application/json").content(validBody(resolved));
            case "PATCH" -> patch(resolved).contentType("application/json").content(validBody(resolved));
            case "DELETE" -> delete(resolved);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        if (role != null) {
            request = request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        mockMvc.perform(request).andExpect(status().is(expected));
    }

    private String validBody(String path) {
        if (path.contains("/stock/")) {
            return "{\"quantity\":1}";
        }
        if (path.startsWith("/api/v1/users")) {
            return "{\"name\":\"Jane\",\"email\":\"jane@example.com\","
                    + "\"password\":\"password123\",\"phone\":\"1234567890\","
                    + "\"department\":\"" + ID + "\"}";
        }
        if (path.startsWith("/api/v1/departments")) {
            return "{\"deptName\":\"D\",\"pocName\":\"P\",\"pocNumber\":123}";
        }
        if (path.startsWith("/api/v1/machine-statuses")) {
            return "{\"name\":\"Special\",\"color\":\"#123456\"}";
        }
        if (path.startsWith("/api/v1/machines")) {
            return "{\"name\":\"M\",\"serialNumber\":\"SN-1\",\"department\":\"" + ID + "\"}";
        }
        if (path.startsWith("/api/v1/spares")) {
            return "{\"name\":\"Bearing\",\"partNumber\":\"BRG-1\"}";
        }
        if (path.contains("/claim")) {
            return "{}";
        }
        if (path.contains("/assignment")) {
            return "{\"assignedSupervisorId\":\"" + ID + "\"}";
        }
        if (path.contains("/updates")) {
            return "{\"status\":\"IN_PROGRESS\",\"description\":\"Started\"}";
        }
        if (path.contains("/costs")) {
            return "{\"category\":\"LABOR\",\"amount\":100}";
        }
        if (path.startsWith("/api/v1/repairs")) {
            return "{\"machineId\":\"" + ID + "\",\"description\":\"Broken\","
                    + "\"repairType\":\"BREAKDOWN\",\"repairPriority\":\"NORMAL\","
                    + "\"idempotencyKey\":\"k-" + ID + "\"}";
        }
        return "{}";
    }

    @Test
    void healthIsReachableWithAnyRole() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER"))))
                .andExpect(status().isOk());
    }

    /**
     * The filter chain denies by default. A newly added route that nobody remembered to
     * map must be refused, not silently served to any authenticated caller. This is the
     * guard that would have caught the missing department and repair-create rules.
     */
    @Test
    void unmappedApiRouteIsRefusedEvenToAManager() throws Exception {
        mockMvc.perform(post("/api/v1/not-yet-implemented")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class, UserController.class, MachineController.class,
            MachineStatusController.class, SpareController.class, RepairController.class,
            DepartmentController.class, HealthController.class
    })
    static class TestApplication {
    }
}
