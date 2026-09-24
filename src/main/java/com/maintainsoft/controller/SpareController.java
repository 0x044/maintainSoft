package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.StockAdjustmentRequest;
import com.maintainsoft.dto.StockIssueRequest;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.dto.StockReturnRequest;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.service.SpareService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spares")
@RequiredArgsConstructor
public class SpareController {

    private final SpareService spareService;

    @GetMapping
    ResponseEntity<List<SpareResponse>> listSpares() {
        return ResponseEntity.ok(spareService.listSpares());
    }

    @GetMapping("/{id}")
    ResponseEntity<SpareResponse> getSpare(@PathVariable UUID id) {
        return ResponseEntity.ok(spareService.getSpare(id));
    }

    @PostMapping
    ResponseEntity<SpareResponse> createSpare(@Valid @RequestBody CreateSpareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spareService.createSpare(request));
    }

    @PatchMapping("/{id}")
    ResponseEntity<SpareResponse> updateSpare(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpareRequest request
    ) {
        return ResponseEntity.ok(spareService.updateSpare(id, request));
    }

    @PostMapping("/{id}/stock/receive")
    ResponseEntity<SpareResponse> receiveStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockQuantityRequest request
    ) {
        return ResponseEntity.ok(spareService.receiveStock(id, request));
    }

    @PostMapping("/{id}/stock/return")
    ResponseEntity<SpareResponse> returnStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockReturnRequest request
    ) {
        return ResponseEntity.ok(spareService.returnStock(id, request));
    }

    @PostMapping("/{id}/stock/adjust")
    ResponseEntity<SpareResponse> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return ResponseEntity.ok(spareService.adjustStock(id, request));
    }

    @PostMapping("/{id}/stock/issue")
    ResponseEntity<SpareResponse> issueStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockIssueRequest request
    ) {
        return ResponseEntity.ok(spareService.issueStock(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> archiveSpare(@PathVariable UUID id) {
        spareService.archiveSpare(id);
        return ResponseEntity.noContent().build();
    }
}
