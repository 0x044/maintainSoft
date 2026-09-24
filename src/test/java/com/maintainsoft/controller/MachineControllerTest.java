package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateMachineRequest;
import com.maintainsoft.dto.MachineResponse;
import com.maintainsoft.dto.UpdateMachineRequest;
import com.maintainsoft.service.MachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MachineControllerTest {

    @Mock
    private MachineService machineService;

    @InjectMocks
    private MachineController machineController;

    @Test
    void listMachinesReturnsFilteredResults() {
        UUID departmentId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        when(machineService.listMachines(departmentId, statusId)).thenReturn(List.of(response()));

        ResponseEntity<List<MachineResponse>> result = machineController.listMachines(departmentId, statusId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(machineService).listMachines(departmentId, statusId);
    }

    @Test
    void createMachineReturnsCreatedResponse() {
        CreateMachineRequest request = createRequest();
        MachineResponse response = response();
        when(machineService.createMachine(request)).thenReturn(response);

        ResponseEntity<MachineResponse> result = machineController.createMachine(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(machineService).createMachine(request);
    }

    @Test
    void updateMachineReturnsUpdatedResponse() {
        UUID id = UUID.randomUUID();
        UpdateMachineRequest request = updateRequest();
        MachineResponse response = response();
        when(machineService.updateMachine(id, request)).thenReturn(response);

        ResponseEntity<MachineResponse> result = machineController.updateMachine(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(machineService).updateMachine(id, request);
    }

    @Test
    void archiveMachineReturnsNoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = machineController.archiveMachine(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(machineService).archiveMachine(id);
    }

    @Test
    void invalidMachineInputIsRejectedAtControllerBoundary() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(machineController)
                    .setValidator(validator)
                    .build();

            mockMvc.perform(post("/api/v1/machines")
                            .contentType("application/json")
                            .content("{\"name\":\"\",\"serialNumber\":\"\",\"departmentId\":null}"))
                    .andExpect(status().isBadRequest());
        } finally {
            validator.destroy();
        }

        verifyNoInteractions(machineService);
    }

    private CreateMachineRequest createRequest() {
        return new CreateMachineRequest(
                "CNC Mill", "SN-001", UUID.randomUUID(), null,
                "Mill", "Acme", "M-1", "Plant 1",
                Instant.parse("2026-01-01T00:00:00Z"), null,
                null, null, 90
        );
    }

    private UpdateMachineRequest updateRequest() {
        return new UpdateMachineRequest(
                "CNC Mill", "SN-001", null, null,
                "Mill", "Acme", "M-1", "Plant 1",
                null, null, null, null, 90
        );
    }

    private MachineResponse response() {
        return new MachineResponse(
                UUID.randomUUID(), "CNC Mill", "SN-001", UUID.randomUUID(), UUID.randomUUID(),
                "Operational", "#22C55E", true, "Mill", "Acme", "M-1", "Plant 1",
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null, 90
        );
    }
}
