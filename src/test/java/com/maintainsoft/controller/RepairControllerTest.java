package com.maintainsoft.controller;

import com.maintainsoft.dto.AssignRepairRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.UpdateRepairRequest;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.service.RepairService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairControllerTest {

    @Mock
    private RepairService repairService;

    @InjectMocks
    private RepairController repairController;

    @Test
    void listRepairsReturnsOk() {
        when(repairService.listRepairs(RepairStatus.OPEN, null)).thenReturn(List.of(response()));

        ResponseEntity<List<RepairResponse>> result = repairController.listRepairs(
                RepairStatus.OPEN, null
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(repairService).listRepairs(RepairStatus.OPEN, null);
    }

    @Test
    void getRepairReturnsOk() {
        UUID id = UUID.randomUUID();
        when(repairService.getRepair(id)).thenReturn(response());

        ResponseEntity<RepairResponse> result = repairController.getRepair(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repairService).getRepair(id);
    }

    @Test
    void createRepairReturnsCreatedAndPassesAuthentication() {
        CreateRepairRequest request = request();
        Authentication authentication = authentication();
        when(repairService.createRepair(request, authentication)).thenReturn(response());

        ResponseEntity<RepairResponse> result = repairController.createRepair(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(repairService).createRepair(request, authentication);
    }

    @Test
    void updateRepairReturnsOk() {
        UUID id = UUID.randomUUID();
        UpdateRepairRequest request = new UpdateRepairRequest(
                "Updated", RepairPriority.LOW, "Technician", "+91-9000000001", null
        );
        Authentication authentication = authentication();
        when(repairService.updateRepair(id, request, authentication)).thenReturn(response());

        ResponseEntity<RepairResponse> result = repairController.updateRepair(id, request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repairService).updateRepair(id, request, authentication);
    }

    @Test
    void claimRepairReturnsOk() {
        UUID id = UUID.randomUUID();
        Authentication authentication = authentication();
        when(repairService.claimRepair(id, authentication)).thenReturn(response());

        ResponseEntity<RepairResponse> result = repairController.claimRepair(id, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repairService).claimRepair(id, authentication);
    }

    @Test
    void assignRepairReturnsOk() {
        UUID id = UUID.randomUUID();
        AssignRepairRequest request = new AssignRepairRequest(UUID.randomUUID());
        Authentication authentication = authentication();
        when(repairService.assignRepair(id, request, authentication)).thenReturn(response());

        ResponseEntity<RepairResponse> result = repairController.assignRepair(id, request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repairService).assignRepair(id, request, authentication);
    }

    private CreateRepairRequest request() {
        return new CreateRepairRequest(
                UUID.randomUUID(),
                RepairType.BREAKDOWN,
                RepairPriority.NORMAL,
                "Machine stopped",
                "breakdown-1",
                "External Technician",
                "+91-9000000000",
                Instant.parse("2026-09-24T04:00:00Z")
        );
    }

    private RepairResponse response() {
        return new RepairResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CNC Mill",
                RepairStatus.OPEN,
                RepairType.BREAKDOWN,
                RepairPriority.NORMAL,
                "Machine stopped",
                Instant.parse("2026-09-24T04:00:00Z"),
                null,
                null,
                null,
                "External Technician",
                "+91-9000000000",
                "breakdown-1"
        );
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(
                "reporter@example.com",
                "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_REPORTER"))
        );
    }
}
