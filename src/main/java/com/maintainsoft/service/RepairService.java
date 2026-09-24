package com.maintainsoft.service;

import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.DuplicateRepairException;
import com.maintainsoft.exception.InvalidRepairException;
import com.maintainsoft.exception.RepairForbiddenException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRepository repairRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RepairResponse> listRepairs(RepairStatus status, UUID machineId) {
        List<Repair> repairs;
        if (status != null && machineId != null) {
            repairs = repairRepository.findByRepairStatusAndMachine_IdOrderByCreatedAtDesc(status, machineId);
        } else if (status != null) {
            repairs = repairRepository.findByRepairStatusOrderByCreatedAtDesc(status);
        } else if (machineId != null) {
            repairs = repairRepository.findByMachine_IdOrderByCreatedAtDesc(machineId);
        } else {
            repairs = repairRepository.findAllByOrderByCreatedAtDesc();
        }
        return repairs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RepairResponse getRepair(UUID id) {
        return repairRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Repair not found: " + id));
    }

    @Transactional
    public RepairResponse createRepair(CreateRepairRequest request, Authentication authentication) {
        validateCreateRequest(request);
        ensureCanCreate(request.repairType(), authentication);

        Repair existing = repairRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Machine not found: " + request.machineId()
                ));

        Repair repair = new Repair();
        repair.setMachine(machine);
        repair.setRepairType(request.repairType());
        repair.setRepairPriority(request.repairPriority() == null
                ? RepairPriority.NORMAL
                : request.repairPriority());
        repair.setDescription(request.description());
        repair.setExternalTechnicianName(request.externalTechnicianName());
        repair.setExternalTechnicianPhone(request.externalTechnicianPhone());
        repair.setStartDate(request.startDate() == null ? Instant.now() : request.startDate());
        repair.setRepairStatus(RepairStatus.OPEN);
        repair.setIdempotencyKey(request.idempotencyKey());

        if (hasRole(authentication, Role.SUPERVISOR)) {
            repair.setAssignedSupervisor(currentUser(authentication));
        }

        try {
            return toResponse(repairRepository.saveAndFlush(repair));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateRepairException("Repair idempotency key already exists");
        }
    }

    private void validateCreateRequest(CreateRepairRequest request) {
        if (request.repairType() == null) {
            throw new InvalidRepairException("repairType is required");
        }
    }

    private void ensureCanCreate(RepairType repairType, Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new RepairForbiddenException("Authentication is required");
        }
        if (repairType != RepairType.BREAKDOWN
                && !hasAnyRole(authentication, Role.MANAGER, Role.SUPERVISOR)) {
            throw new RepairForbiddenException("Only managers and supervisors can create scheduled repairs");
        }
    }

    private boolean hasAnyRole(Authentication authentication, Role... roles) {
        for (Role role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(Authentication authentication, Role role) {
        String expectedAuthority = "ROLE_" + role.name();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expectedAuthority.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + authentication.getName()
                ));
    }

    private RepairResponse toResponse(Repair repair) {
        User assignedSupervisor = repair.getAssignedSupervisor();
        return new RepairResponse(
                repair.getId(),
                repair.getMachine().getId(),
                repair.getMachine().getName(),
                repair.getRepairStatus(),
                repair.getRepairType(),
                repair.getRepairPriority(),
                repair.getDescription(),
                repair.getStartDate(),
                repair.getEndDate(),
                assignedSupervisor == null ? null : assignedSupervisor.getId(),
                assignedSupervisor == null ? null : assignedSupervisor.getName(),
                repair.getExternalTechnicianName(),
                repair.getExternalTechnicianPhone(),
                repair.getIdempotencyKey()
        );
    }
}
