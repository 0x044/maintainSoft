package com.maintainsoft.service;

import com.maintainsoft.dto.AddRepairCostRequest;
import com.maintainsoft.dto.AddRepairUpdateRequest;
import com.maintainsoft.dto.AssignRepairRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairCostResponse;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.RepairUpdateResponse;
import com.maintainsoft.dto.UpdateRepairRequest;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.RepairCost;
import com.maintainsoft.entity.RepairUpdate;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.DuplicateRepairException;
import com.maintainsoft.exception.InvalidRepairException;
import com.maintainsoft.exception.RepairConflictException;
import com.maintainsoft.exception.RepairForbiddenException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.RepairCostRepository;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.RepairUpdateRepository;
import com.maintainsoft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRepository repairRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final RepairUpdateRepository repairUpdateRepository;
    private final RepairCostRepository repairCostRepository;

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

    @Transactional
    public RepairResponse updateRepair(UUID id, UpdateRepairRequest request, Authentication authentication) {
        ensureCanManage(authentication);
        Repair repair = findRepair(id);

        if (request.description() != null) {
            repair.setDescription(request.description());
        }
        if (request.repairPriority() != null) {
            repair.setRepairPriority(request.repairPriority());
        }
        if (request.externalTechnicianName() != null) {
            repair.setExternalTechnicianName(request.externalTechnicianName());
        }
        if (request.externalTechnicianPhone() != null) {
            repair.setExternalTechnicianPhone(request.externalTechnicianPhone());
        }
        if (request.startDate() != null) {
            repair.setStartDate(request.startDate());
        }

        return toResponse(repairRepository.saveAndFlush(repair));
    }

    @Transactional
    public RepairResponse claimRepair(UUID id, Authentication authentication) {
        ensureSupervisor(authentication);
        User supervisor = currentUser(authentication);
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new RepairForbiddenException("Only supervisors can claim repairs");
        }

        Repair repair = findRepair(id);
        User assignedSupervisor = repair.getAssignedSupervisor();
        if (assignedSupervisor != null) {
            if (assignedSupervisor.getId().equals(supervisor.getId())) {
                return toResponse(repair);
            }
            throw new RepairConflictException("Repair is already assigned to another supervisor");
        }

        repair.setAssignedSupervisor(supervisor);
        return toResponse(repairRepository.saveAndFlush(repair));
    }

    @Transactional
    public RepairResponse assignRepair(UUID id, AssignRepairRequest request, Authentication authentication) {
        ensureManager(authentication);
        Repair repair = findRepair(id);

        if (request.assignedSupervisorId() == null) {
            repair.setAssignedSupervisor(null);
        } else {
            User supervisor = userRepository.findById(request.assignedSupervisorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supervisor not found: " + request.assignedSupervisorId()
                    ));
            if (supervisor.getRole() != Role.SUPERVISOR) {
                throw new InvalidRepairException("Repair can only be assigned to a supervisor");
            }
            repair.setAssignedSupervisor(supervisor);
        }

        return toResponse(repairRepository.saveAndFlush(repair));
    }

    @Transactional(readOnly = true)
    public List<RepairUpdateResponse> listRepairUpdates(UUID id) {
        findRepair(id);
        return repairUpdateRepository.findByRepair_IdOrderByCreatedAtAsc(id).stream()
                .map(this::toUpdateResponse)
                .toList();
    }

    @Transactional
    public RepairUpdateResponse addRepairUpdate(
            UUID id,
            AddRepairUpdateRequest request,
            Authentication authentication
    ) {
        if (request == null || request.status() == null
                || request.description() == null || request.description().isBlank()) {
            throw new InvalidRepairException("status and description are required");
        }

        Repair repair = findRepair(id);
        ensureCanPost(authentication, repair);
        validateStatusTransition(repair.getRepairStatus(), request.status());

        if (repair.getRepairStatus() != request.status()) {
            repair.setRepairStatus(request.status());
            if (request.status() == RepairStatus.COMPLETED) {
                repair.setEndDate(Instant.now());
            }
            repairRepository.save(repair);
        }

        RepairUpdate update = new RepairUpdate();
        update.setRepair(repair);
        update.setRepairStatus(request.status());
        update.setDescription(request.description());
        repairUpdateRepository.save(update);
        return toUpdateResponse(update);
    }

    @Transactional(readOnly = true)
    public List<RepairCostResponse> listRepairCosts(UUID id) {
        findRepair(id);
        return repairCostRepository.findByRepair_IdOrderByCreatedAtAsc(id).stream()
                .map(this::toCostResponse)
                .toList();
    }

    @Transactional
    public RepairCostResponse addRepairCost(
            UUID id,
            AddRepairCostRequest request,
            Authentication authentication
    ) {
        if (request == null || request.category() == null || request.amount() == null) {
            throw new InvalidRepairException("category and amount are required");
        }

        Repair repair = findRepair(id);
        ensureCanPost(authentication, repair);
        BigDecimal amount = normalizeAmount(request.amount());

        RepairCost cost = new RepairCost();
        cost.setRepair(repair);
        cost.setCategory(request.category());
        cost.setAmount(amount);
        cost.setDescription(request.description());
        repairCostRepository.save(cost);
        return toCostResponse(cost);
    }

    private void ensureCanPost(Authentication authentication, Repair repair) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new RepairForbiddenException("Authentication is required");
        }
        if (hasRole(authentication, Role.MANAGER)) {
            return;
        }
        if (!hasRole(authentication, Role.SUPERVISOR)) {
            throw new RepairForbiddenException("Only the assigned supervisor can post repair updates");
        }

        User currentSupervisor = currentUser(authentication);
        User assignedSupervisor = repair.getAssignedSupervisor();
        if (assignedSupervisor == null
                || !Objects.equals(assignedSupervisor.getId(), currentSupervisor.getId())) {
            throw new RepairForbiddenException("Only the assigned supervisor can post repair updates");
        }
    }

    private void validateStatusTransition(RepairStatus current, RepairStatus requested) {
        if (current == null || requested == null) {
            throw new InvalidRepairException("Repair status is required");
        }
        if (current == requested) {
            return;
        }
        boolean valid = (current == RepairStatus.OPEN && requested == RepairStatus.IN_PROGRESS)
                || (current == RepairStatus.IN_PROGRESS && requested == RepairStatus.COMPLETED);
        if (!valid) {
            throw new InvalidRepairException(
                    "Repair status must move from " + current + " to " + requested
            );
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new InvalidRepairException("Cost amount cannot be negative");
        }
        try {
            BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.precision() - normalized.scale() > 17) {
                throw new InvalidRepairException("Cost amount is too large");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new InvalidRepairException("Cost amount must have at most two decimal places");
        }
    }

    private Repair findRepair(UUID id) {
        return repairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair not found: " + id));
    }

    private void ensureCanManage(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null
                || !hasAnyRole(authentication, Role.MANAGER, Role.SUPERVISOR)) {
            throw new RepairForbiddenException("Only managers and supervisors can manage repairs");
        }
    }

    private void ensureSupervisor(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null
                || !hasRole(authentication, Role.SUPERVISOR)) {
            throw new RepairForbiddenException("Only supervisors can claim repairs");
        }
    }

    private void ensureManager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null
                || !hasRole(authentication, Role.MANAGER)) {
            throw new RepairForbiddenException("Only managers can assign repairs");
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

    private RepairUpdateResponse toUpdateResponse(RepairUpdate update) {
        return new RepairUpdateResponse(
                update.getId(),
                update.getRepair().getId(),
                update.getRepairStatus(),
                update.getDescription(),
                update.getCreatedAt(),
                update.getCreatedBy()
        );
    }

    private RepairCostResponse toCostResponse(RepairCost cost) {
        return new RepairCostResponse(
                cost.getId(),
                cost.getRepair().getId(),
                cost.getCategory(),
                cost.getAmount(),
                "INR",
                cost.getDescription(),
                cost.getCreatedAt(),
                cost.getCreatedBy()
        );
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
