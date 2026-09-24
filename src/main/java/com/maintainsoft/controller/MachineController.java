package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateMachineRequest;
import com.maintainsoft.dto.MachineResponse;
import com.maintainsoft.dto.UpdateMachineRequest;
import com.maintainsoft.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    ResponseEntity<List<MachineResponse>> listMachines(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID statusId
    ) {
        return ResponseEntity.ok(machineService.listMachines(departmentId, statusId));
    }

    @GetMapping("/{id}")
    ResponseEntity<MachineResponse> getMachine(@PathVariable UUID id) {
        return ResponseEntity.ok(machineService.getMachine(id));
    }

    @PostMapping
    ResponseEntity<MachineResponse> createMachine(@Valid @RequestBody CreateMachineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(machineService.createMachine(request));
    }

    @PatchMapping("/{id}")
    ResponseEntity<MachineResponse> updateMachine(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMachineRequest request
    ) {
        return ResponseEntity.ok(machineService.updateMachine(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> archiveMachine(@PathVariable UUID id) {
        machineService.archiveMachine(id);
        return ResponseEntity.noContent().build();
    }
}
