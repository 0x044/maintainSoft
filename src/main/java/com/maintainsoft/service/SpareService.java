package com.maintainsoft.service;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.exception.DuplicateSpareException;
import com.maintainsoft.exception.InvalidSpareException;
import com.maintainsoft.exception.ResourceNotFoundException;
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
