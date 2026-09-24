package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.MachineStatusResponse;
import com.maintainsoft.service.MachineStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MachineStatusControllerTest {

    @Mock
    private MachineStatusService machineStatusService;

    @InjectMocks
    private MachineStatusController machineStatusController;

    @Test
    void createCustomStatusReturnsCreatedResponse() {
        CreateMachineStatusRequest request = new CreateMachineStatusRequest("Awaiting parts", "#8B5CF6");
        MachineStatusResponse response = new MachineStatusResponse(
                UUID.randomUUID(), "Awaiting parts", "#8B5CF6", false
        );
        when(machineStatusService.createCustomStatus(request)).thenReturn(response);

        ResponseEntity<MachineStatusResponse> result = machineStatusController.createCustomStatus(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(machineStatusService).createCustomStatus(request);
    }

    @Test
    void invalidColorIsRejectedAtControllerBoundary() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(machineStatusController)
                    .setValidator(validator)
                    .build();

            mockMvc.perform(post("/api/v1/machine-statuses")
                            .contentType("application/json")
                            .content("{\"name\":\"Awaiting parts\",\"color\":\"purple\"}"))
                    .andExpect(status().isBadRequest());
        } finally {
            validator.destroy();
        }

        verifyNoInteractions(machineStatusService);
    }
}
