package com.maintainsoft.service;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.StockAdjustmentRequest;
import com.maintainsoft.dto.StockIssueRequest;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.dto.StockReturnRequest;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.RepairSpare;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.exception.DuplicateSpareException;
import com.maintainsoft.exception.InvalidSpareException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.RepairSpareRepository;
import com.maintainsoft.repository.SpareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpareService {

    private final SpareRepository spareRepository;
    private final RepairRepository repairRepository;
    private final RepairSpareRepository repairSpareRepository;

    @Transactional(readOnly = true)
    public List<SpareResponse> listSpares() {
        return spareRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpareResponse getSpare(UUID id) {
        return spareRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Spare not found: " + id));
    }

    @Transactional
    public SpareResponse createSpare(CreateSpareRequest request) {
        validateStock(request.initialStock());
        Spare spare = new Spare();
        spare.setPartNumber(request.partNumber());
        spare.setName(request.name());
        spare.setDescription(request.description());
        spare.setUnit(request.unit());
        spare.setCompatibleMachine(request.compatibleMachine());
        spare.setStock(request.initialStock());

        try {
            return toResponse(spareRepository.saveAndFlush(spare));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSpareException("Spare part number already exists");
        }
    }

    @Transactional
    public SpareResponse updateSpare(UUID id, UpdateSpareRequest request) {
        Spare spare = spareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spare not found: " + id));
        if (request.partNumber() != null) {
            spare.setPartNumber(request.partNumber());
        }
        if (request.name() != null) {
            spare.setName(request.name());
        }
        if (request.description() != null) {
            spare.setDescription(request.description());
        }
        if (request.unit() != null) {
            spare.setUnit(request.unit());
        }
        if (request.compatibleMachine() != null) {
            spare.setCompatibleMachine(request.compatibleMachine());
        }

        try {
            return toResponse(spareRepository.saveAndFlush(spare));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSpareException("Spare part number already exists");
        }
    }

    @Transactional
    public void archiveSpare(UUID id) {
        Spare spare = spareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spare not found: " + id));
        spare.setDeleted(true);
        spareRepository.save(spare);
    }

    @Transactional
    public SpareResponse receiveStock(UUID id, StockQuantityRequest request) {
        validatePositiveQuantity(request.quantity());
        Spare spare = getLockedSpare(id);
        spare.setStock(addWithoutOverflow(spare.getStock(), request.quantity()));
        return toResponse(spareRepository.save(spare));
    }

    @Transactional
    public SpareResponse returnStock(UUID id, StockReturnRequest request) {
        validatePositiveQuantity(request.quantity());
        Spare spare = getLockedSpare(id);
        getRepair(request.repairId());
        RepairSpare repairSpare = getRepairSpare(request.repairId(), id)
                .orElseThrow(() -> new InvalidSpareException("Spare has not been issued to this repair"));
        if (repairSpare.getUsedQuantity() < request.quantity()) {
            throw new InvalidSpareException("Return quantity exceeds the quantity issued to this repair");
        }

        repairSpare.setUsedQuantity(repairSpare.getUsedQuantity() - request.quantity());
        repairSpareRepository.save(repairSpare);
        spare.setStock(addWithoutOverflow(spare.getStock(), request.quantity()));
        return toResponse(spareRepository.save(spare));
    }

    @Transactional
    public SpareResponse adjustStock(UUID id, StockAdjustmentRequest request) {
        if (request.quantity() < 0) {
            throw new InvalidSpareException("Stock quantity cannot be negative");
        }
        Spare spare = getLockedSpare(id);
        spare.setStock(request.quantity());
        return toResponse(spareRepository.save(spare));
    }

    @Transactional
    public SpareResponse issueStock(UUID id, StockIssueRequest request) {
        validatePositiveQuantity(request.quantity());
        Spare spare = getLockedSpare(id);
        if (spare.getStock() < request.quantity()) {
            throw new InvalidSpareException("Insufficient spare stock");
        }
        Repair repair = getRepair(request.repairId());
        RepairSpare repairSpare = getRepairSpare(request.repairId(), id)
                .orElseGet(() -> newRepairSpare(repair, spare));
        repairSpare.setUsedQuantity(addRepairUsageWithoutOverflow(
                repairSpare.getUsedQuantity(),
                request.quantity()
        ));
        repairSpareRepository.save(repairSpare);
        spare.setStock(spare.getStock() - request.quantity());
        return toResponse(spareRepository.save(spare));
    }

    private Spare getLockedSpare(UUID id) {
        return spareRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spare not found: " + id));
    }

    private Repair getRepair(UUID id) {
        return repairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair not found: " + id));
    }

    private java.util.Optional<RepairSpare> getRepairSpare(UUID repairId, UUID spareId) {
        RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();
        id.setRepairId(repairId);
        id.setSpareId(spareId);
        return repairSpareRepository.findById(id);
    }

    private RepairSpare newRepairSpare(Repair repair, Spare spare) {
        RepairSpare repairSpare = new RepairSpare();
        repairSpare.getId().setRepairId(repair.getId());
        repairSpare.getId().setSpareId(spare.getId());
        repairSpare.setRepair(repair);
        repairSpare.setSpare(spare);
        return repairSpare;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidSpareException("Stock quantity must be positive");
        }
    }

    private int addRepairUsageWithoutOverflow(int current, int quantity) {
        try {
            return Math.addExact(current, quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidSpareException("Repair spare usage is too large");
        }
    }

    private int addWithoutOverflow(int current, int quantity) {
        try {
            return Math.addExact(current, quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidSpareException("Stock quantity is too large");
        }
    }

    private void validateStock(int stock) {
        if (stock < 0) {
            throw new InvalidSpareException("initialStock cannot be negative");
        }
    }

    private SpareResponse toResponse(Spare spare) {
        return new SpareResponse(
                spare.getId(),
                spare.getPartNumber(),
                spare.getName(),
                spare.getDescription(),
                spare.getUnit(),
                spare.getCompatibleMachine(),
                spare.getStock()
        );
    }
}
