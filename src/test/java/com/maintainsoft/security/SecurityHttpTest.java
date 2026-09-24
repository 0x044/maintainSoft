package com.maintainsoft.security;

import com.maintainsoft.controller.AuthController;
import com.maintainsoft.controller.MachineController;
import com.maintainsoft.controller.MachineStatusController;
import com.maintainsoft.controller.RepairController;
import com.maintainsoft.controller.SpareController;
import com.maintainsoft.controller.UserController;
import com.maintainsoft.dto.AddRepairUpdateRequest;
import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.MachineStatusResponse;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.RepairUpdateResponse;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.enums.Role;
import com.maintainsoft.service.AuthService;
import com.maintainsoft.service.MachineService;
import com.maintainsoft.service.MachineStatusService;
import com.maintainsoft.service.RepairService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityHttpTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private MachineService machineService;

    @MockitoBean
    private MachineStatusService machineStatusService;

    @MockitoBean
    private SpareService spareService;

    @MockitoBean
    private RepairService repairService;

    @Test
    void unauthenticatedUserCreationIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userManagementService);
    }

    @Test
    void supervisorCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userManagementService);
    }

    @Test
    void managerCanCreateUsers() throws Exception {
        UUID departmentId = UUID.randomUUID();
        when(userManagementService.createSupervisor(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(
                        UUID.randomUUID(), "Jane Doe", "jane@example.com", "1234567890",
                        Role.SUPERVISOR, departmentId
                ));

        mockMvc.perform(post("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void reporterCannotReadManagerSupervisorMachineRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/machines")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(machineService);
    }

    @Test
    void supervisorCanReadMachineRoutes() throws Exception {
        when(machineService.listMachines(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/machines")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void anyAuthenticatedUserCanReadRepairRoutes() throws Exception {
        when(repairService.listRepairs(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repairs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER"))))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRepairReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/repairs"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(repairService);
    }

    @Test
    void supervisorCannotCreateCustomMachineStatus() throws Exception {
        mockMvc.perform(post("/api/v1/machine-statuses")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Special\",\"color\":\"#123456\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(machineStatusService);
    }

    @Test
    void managerCanCreateCustomMachineStatus() throws Exception {
        when(machineStatusService.createCustomStatus(any(CreateMachineStatusRequest.class)))
                .thenReturn(new MachineStatusResponse(UUID.randomUUID(), "Special", "#123456", false));

        mockMvc.perform(post("/api/v1/machine-statuses")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Special\",\"color\":\"#123456\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void reporterCannotReceiveStock() throws Exception {
        mockMvc.perform(post("/api/v1/spares/{id}/stock/receive", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(spareService);
    }

    @Test
    void supervisorCanReceiveStock() throws Exception {
        UUID id = UUID.randomUUID();
        when(spareService.receiveStock(eq(id), any(StockQuantityRequest.class)))
                .thenReturn(spareResponse());

        mockMvc.perform(post("/api/v1/spares/{id}/stock/receive", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void reporterCannotClaimRepair() throws Exception {
        mockMvc.perform(post("/api/v1/repairs/{id}/claim", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repairService);
    }

    @Test
    void supervisorCanClaimRepair() throws Exception {
        UUID id = UUID.randomUUID();
        when(repairService.claimRepair(eq(id), any())).thenReturn(repairResponse());

        mockMvc.perform(post("/api/v1/repairs/{id}/claim", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void supervisorCannotAssignRepair() throws Exception {
        mockMvc.perform(patch("/api/v1/repairs/{id}/assignment", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedSupervisorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repairService);
    }

    @Test
    void reporterCannotPostRepairUpdate() throws Exception {
        mockMvc.perform(post("/api/v1/repairs/{id}/updates", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"description\":\"Started\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repairService);
    }

    @Test
    void supervisorCanPostRepairUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        when(repairService.addRepairUpdate(eq(id), any(AddRepairUpdateRequest.class), any()))
                .thenReturn(updateResponse());

        mockMvc.perform(post("/api/v1/repairs/{id}/updates", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"description\":\"Started\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void managerCanAssignRepair() throws Exception {
        UUID id = UUID.randomUUID();
        when(repairService.assignRepair(eq(id), any(), any())).thenReturn(repairResponse());

        mockMvc.perform(patch("/api/v1/repairs/{id}/assignment", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedSupervisorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedLogoutIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLogoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REPORTER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isNoContent());
    }

    private SpareResponse spareResponse() {
        return new SpareResponse(UUID.randomUUID(), "BRG-001", "Bearing", null, "piece", null, 4);
    }

    private RepairResponse repairResponse() {
        return new RepairResponse(
                UUID.randomUUID(), UUID.randomUUID(), "CNC Mill", RepairStatus.OPEN,
                RepairType.BREAKDOWN, RepairPriority.NORMAL, "Machine stopped",
                Instant.parse("2026-09-24T04:00:00Z"), null, null, null,
                null, null, "repair-1"
        );
    }

    private RepairUpdateResponse updateResponse() {
        return new RepairUpdateResponse(
                UUID.randomUUID(), UUID.randomUUID(), RepairStatus.IN_PROGRESS,
                "Started", Instant.now(), "supervisor@example.com"
        );
    }

    private String validUserJson() {
        return """
                {
                  "name": "Jane Doe",
                  "email": "jane@example.com",
                  "password": "password123",
                  "phone": "1234567890",
                  "department": "%s"
                }
                """.formatted(UUID.randomUUID());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            AuthController.class,
            UserController.class,
            MachineController.class,
            MachineStatusController.class,
            SpareController.class,
            RepairController.class
    })
    static class TestApplication {
    }
}
