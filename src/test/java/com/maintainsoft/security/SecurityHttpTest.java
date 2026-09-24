package com.maintainsoft.security;

import com.maintainsoft.controller.MachineController;
import com.maintainsoft.controller.RepairController;
import com.maintainsoft.controller.UserController;
import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.enums.Role;
import com.maintainsoft.service.MachineService;
import com.maintainsoft.service.RepairService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityHttpTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private MachineService machineService;

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
    @Import({SecurityConfig.class, UserController.class, MachineController.class, RepairController.class})
    static class TestApplication {
    }
}
