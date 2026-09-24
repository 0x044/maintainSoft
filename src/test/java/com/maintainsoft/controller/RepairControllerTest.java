package com.maintainsoft.controller;

import com.maintainsoft.dto.AddRepairCostRequest;
import com.maintainsoft.dto.AddRepairUpdateRequest;
import com.maintainsoft.dto.AssignRepairRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairCostResponse;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.RepairUpdateResponse;
import com.maintainsoft.dto.UpdateRepairRequest;
import com.maintainsoft.enums.RepairCostCategory;
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

import java.math.BigDecimal;
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

    @Test
    void addRepairUpdateReturnsCreated() {
        UUID id = UUID.randomUUID();
        AddRepairUpdateRequest request = new AddRepairUpdateRequest(
                RepairStatus.IN_PROGRESS, "Technician started"
        );
        Authentication authentication = authentication();
        when(repairService.addRepairUpdate(id, request, authentication)).thenReturn(updateResponse());

        ResponseEntity<RepairUpdateResponse> result = repairController.addRepairUpdate(
                id, request, authentication
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(repairService).addRepairUpdate(id, request, authentication);
    }

    @Test
    void listRepairUpdatesReturnsOk() {
        UUID id = UUID.randomUUID();
        when(repairService.listRepairUpdates(id)).thenReturn(List.of(updateResponse()));

        ResponseEntity<List<RepairUpdateResponse>> result = repairController.listRepairUpdates(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void addRepairCostReturnsCreated() {
        UUID id = UUID.randomUUID();
        AddRepairCostRequest request = new AddRepairCostRequest(
                RepairCostCategory.LABOR, new BigDecimal("100.00"), "Labor"
        );
        Authentication authentication = authentication();
        when(repairService.addRepairCost(id, request, authentication)).thenReturn(costResponse());

        ResponseEntity<RepairCostResponse> result = repairController.addRepairCost(
                id, request, authentication
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(repairService).addRepairCost(id, request, authentication);
    }

    @Test
    void listRepairCostsReturnsOk() {
        UUID id = UUID.randomUUID();
        when(repairService.listRepairCosts(id)).thenReturn(List.of(costResponse()));

        ResponseEntity<List<RepairCostResponse>> result = repairController.listRepairCosts(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
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

    private RepairUpdateResponse updateResponse() {
        return new RepairUpdateResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                RepairStatus.IN_PROGRESS,
                "Technician started",
                Instant.parse("2026-09-24T05:00:00Z"),
                "supervisor@example.com"
        );
    }

    private RepairCostResponse costResponse() {
        return new RepairCostResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                RepairCostCategory.LABOR,
                new BigDecimal("100.00"),
                "INR",
                "Labor",
                Instant.parse("2026-09-24T05:00:00Z"),
                "manager@example.com"
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
