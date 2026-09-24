package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.MachineStatusResponse;
import com.maintainsoft.service.MachineStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machine-statuses")
@RequiredArgsConstructor
public class MachineStatusController {

    private final MachineStatusService machineStatusService;

    @GetMapping
    ResponseEntity<List<MachineStatusResponse>> listStatuses() {
        return ResponseEntity.ok(machineStatusService.listStatuses());
    }

    @PostMapping
    ResponseEntity<MachineStatusResponse> createCustomStatus(
            @Valid @RequestBody CreateMachineStatusRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(machineStatusService.createCustomStatus(request));
    }
}
