package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.UserResponse;
import com.maintainsoft.enums.Role;
import com.maintainsoft.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserManagementService userManagementService;

    @InjectMocks
    private UserController userController;

    @Test
    void createSupervisorReturnsCreatedResponse() {
        CreateUserRequest request = request();
        UserResponse response = new UserResponse(
                UUID.randomUUID(), request.name(), request.email(), request.phone(),
                Role.SUPERVISOR, request.department()
        );
        when(userManagementService.createSupervisor(request)).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.createSupervisor(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(userManagementService).createSupervisor(request);
    }

    @Test
    void invalidCreateUserRequestIsRejectedAtControllerBoundary() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(userController)
                    .setValidator(validator)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"x\",\"phone\":\"\",\"department\":null}"))
                    .andExpect(status().isBadRequest());
        } finally {
            validator.destroy();
        }

        verifyNoInteractions(userManagementService);
    }

    private CreateUserRequest request() {
        return new CreateUserRequest(
                "Jane Doe",
                "jane@example.com",
                "password123",
                "1234567890",
                UUID.randomUUID()
        );
    }
}
