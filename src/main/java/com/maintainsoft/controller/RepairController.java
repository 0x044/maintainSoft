package com.maintainsoft.controller;

import com.maintainsoft.dto.AssignRepairRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.UpdateRepairRequest;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.service.RepairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final RepairService repairService;

    @GetMapping
    ResponseEntity<List<RepairResponse>> listRepairs(
            @RequestParam(required = false) RepairStatus status,
            @RequestParam(required = false) UUID machineId
    ) {
        return ResponseEntity.ok(repairService.listRepairs(status, machineId));
    }

    @GetMapping("/{id}")
    ResponseEntity<RepairResponse> getRepair(@PathVariable UUID id) {
        return ResponseEntity.ok(repairService.getRepair(id));
    }

    @PostMapping
    ResponseEntity<RepairResponse> createRepair(
            @Valid @RequestBody CreateRepairRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                repairService.createRepair(request, authentication)
        );
    }

    @PatchMapping("/{id}")
    ResponseEntity<RepairResponse> updateRepair(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRepairRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repairService.updateRepair(id, request, authentication));
    }

    @PostMapping("/{id}/claim")
    ResponseEntity<RepairResponse> claimRepair(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repairService.claimRepair(id, authentication));
    }

    @PatchMapping("/{id}/assignment")
    ResponseEntity<RepairResponse> assignRepair(
            @PathVariable UUID id,
            @RequestBody AssignRepairRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(repairService.assignRepair(id, request, authentication));
    }
}
