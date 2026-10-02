package com.maintainsoft.service;

import com.maintainsoft.dto.CreateMachineRequest;
import com.maintainsoft.dto.MachineResponse;
import com.maintainsoft.dto.UpdateMachineRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.MachineStatus;
import com.maintainsoft.exception.DuplicateMachineException;
import com.maintainsoft.exception.InvalidMachineException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.MachineStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
public class MachineService {

    private static final String OPERATIONAL_STATUS_KEY = "OPERATIONAL";

    private final MachineRepository machineRepository;
    private final DepartmentRepository departmentRepository;
    private final MachineStatusRepository machineStatusRepository;

    @Transactional(readOnly = true)
    public List<MachineResponse> listMachines(UUID departmentId, UUID statusId) {
        List<Machine> machines;
        if (departmentId != null && statusId != null) {
            machines = machineRepository.findByDepartment_IdAndStatus_IdOrderByNameAsc(departmentId, statusId);
        } else if (departmentId != null) {
            machines = machineRepository.findByDepartment_IdOrderByNameAsc(departmentId);
        } else if (statusId != null) {
            machines = machineRepository.findByStatus_IdOrderByNameAsc(statusId);
        } else {
            machines = machineRepository.findAllByOrderByNameAsc();
        }
        return machines.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachine(UUID id) {
        return machineRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR')")
public MachineResponse createMachine(CreateMachineRequest request) {
        validateLifecycle(request.commissionedAt(), request.decommissionedAt());
        validateMaintenanceInterval(request.maintenanceIntervalDays());
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found: " + request.departmentId()
                ));
        MachineStatus status = resolveStatus(request.statusId());

        Machine machine = new Machine();
        machine.setName(request.name());
        machine.setSerialNumber(request.serialNumber());
        machine.setDepartment(department);
        machine.setStatus(status);
        applyOptionalFields(machine, request.equipmentType(), request.manufacturer(), request.model(),
                request.location(), request.commissionedAt(), request.decommissionedAt(),
                request.lastServicedAt(), request.nextServiceDueAt(), request.maintenanceIntervalDays());

        try {
            return toResponse(machineRepository.saveAndFlush(machine));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateMachineException("Machine serial number already exists");
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR')")
public MachineResponse updateMachine(UUID id, UpdateMachineRequest request) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
        validateLifecycle(
                request.commissionedAt() != null ? request.commissionedAt() : machine.getCommissionedAt(),
                request.decommissionedAt() != null ? request.decommissionedAt() : machine.getDecommissionedAt()
        );
        validateMaintenanceInterval(request.maintenanceIntervalDays());

        if (request.departmentId() != null) {
            machine.setDepartment(departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found: " + request.departmentId()
                    )));
        }
        if (request.statusId() != null) {
            machine.setStatus(resolveStatus(request.statusId()));
        }
        if (request.name() != null) {
            machine.setName(request.name());
        }
        if (request.serialNumber() != null) {
            machine.setSerialNumber(request.serialNumber());
        }
        applyOptionalFields(machine, request.equipmentType(), request.manufacturer(), request.model(),
                request.location(), request.commissionedAt(), request.decommissionedAt(),
                request.lastServicedAt(), request.nextServiceDueAt(), request.maintenanceIntervalDays());

        try {
            return toResponse(machineRepository.saveAndFlush(machine));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateMachineException("Machine serial number already exists");
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR')")
public void archiveMachine(UUID id) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
        machine.setDeleted(true);
        machineRepository.save(machine);
    }

    private MachineStatus resolveStatus(UUID statusId) {
        if (statusId != null) {
            return machineStatusRepository.findById(statusId)
                    .orElseThrow(() -> new ResourceNotFoundException("Machine status not found: " + statusId));
        }
        return machineStatusRepository.findBySystemKey(OPERATIONAL_STATUS_KEY)
                .orElseThrow(() -> new ResourceNotFoundException("Operational machine status is not configured"));
    }

    private void applyOptionalFields(
            Machine machine,
            String equipmentType,
            String manufacturer,
            String model,
            String location,
            java.time.Instant commissionedAt,
            java.time.Instant decommissionedAt,
            java.time.Instant lastServicedAt,
            java.time.Instant nextServiceDueAt,
            Integer maintenanceIntervalDays
    ) {
        if (equipmentType != null) {
            machine.setEquipmentType(equipmentType);
        }
        if (manufacturer != null) {
            machine.setManufacturer(manufacturer);
        }
        if (model != null) {
            machine.setModel(model);
        }
        if (location != null) {
            machine.setLocation(location);
        }
        if (commissionedAt != null) {
            machine.setCommissionedAt(commissionedAt);
        }
        if (decommissionedAt != null) {
            machine.setDecommissionedAt(decommissionedAt);
        }
        if (lastServicedAt != null) {
            machine.setLastServicedAt(lastServicedAt);
        }
        if (nextServiceDueAt != null) {
            machine.setNextServiceDueAt(nextServiceDueAt);
        }
        if (maintenanceIntervalDays != null) {
            machine.setMaintenanceIntervalDays(maintenanceIntervalDays);
        }
    }

    private void validateMaintenanceInterval(Integer maintenanceIntervalDays) {
        if (maintenanceIntervalDays != null && maintenanceIntervalDays <= 0) {
            throw new InvalidMachineException("maintenanceIntervalDays must be positive");
        }
    }

    private void validateLifecycle(java.time.Instant commissionedAt, java.time.Instant decommissionedAt) {
        if (commissionedAt != null && decommissionedAt != null
                && decommissionedAt.isBefore(commissionedAt)) {
            throw new InvalidMachineException("decommissionedAt cannot be before commissionedAt");
        }
    }

    private MachineResponse toResponse(Machine machine) {
        MachineStatus status = machine.getStatus();
        return new MachineResponse(
                machine.getId(),
                machine.getName(),
                machine.getSerialNumber(),
                machine.getDepartment().getId(),
                status.getId(),
                status.getName(),
                status.getColor(),
                status.isBuiltIn(),
                machine.getEquipmentType(),
                machine.getManufacturer(),
                machine.getModel(),
                machine.getLocation(),
                machine.getCommissionedAt(),
                machine.getDecommissionedAt(),
                machine.getLastServicedAt(),
                machine.getNextServiceDueAt(),
                machine.getMaintenanceIntervalDays()
        );
    }
}
