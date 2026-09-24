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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private MachineStatusRepository machineStatusRepository;

    @InjectMocks
    private MachineService machineService;

    @Test
    void createsMachineWithDefaultOperationalStatusAndMasterFields() {
        UUID departmentId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        Department department = department(departmentId);
        MachineStatus operational = status(statusId, "Operational", "#22C55E", true, "OPERATIONAL");
        CreateMachineRequest request = createRequest(departmentId, null);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(machineStatusRepository.findBySystemKey("OPERATIONAL")).thenReturn(Optional.of(operational));
        when(machineRepository.saveAndFlush(any(Machine.class))).thenAnswer(invocation -> {
            Machine machine = invocation.getArgument(0);
            machine.setId(UUID.randomUUID());
            return machine;
        });

        MachineResponse response = machineService.createMachine(request);

        assertThat(response.name()).isEqualTo("CNC Mill");
        assertThat(response.serialNumber()).isEqualTo("SN-001");
        assertThat(response.statusId()).isEqualTo(statusId);
        assertThat(response.statusBuiltIn()).isTrue();
        assertThat(response.manufacturer()).isEqualTo("Acme");
        assertThat(response.maintenanceIntervalDays()).isEqualTo(90);
    }

    @Test
    void createsMachineWithExplicitCustomStatus() {
        UUID departmentId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        Department department = department(departmentId);
        MachineStatus custom = status(statusId, "Awaiting parts", "#8B5CF6", false, null);
        CreateMachineRequest request = createRequest(departmentId, statusId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(machineStatusRepository.findById(statusId)).thenReturn(Optional.of(custom));
        when(machineRepository.saveAndFlush(any(Machine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MachineResponse response = machineService.createMachine(request);

        assertThat(response.statusId()).isEqualTo(statusId);
        assertThat(response.statusBuiltIn()).isFalse();
    }

    @Test
    void rejectsUnknownDepartmentAndStatus() {
        UUID departmentId = UUID.randomUUID();
        CreateMachineRequest request = createRequest(departmentId, null);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.createMachine(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(machineStatusRepository, never()).findBySystemKey(any());
    }

    @Test
    void rejectsInvalidLifecycleDates() {
        UUID departmentId = UUID.randomUUID();
        Instant commissioned = Instant.parse("2026-01-01T00:00:00Z");
        Instant decommissioned = commissioned.minusSeconds(1);
        CreateMachineRequest request = new CreateMachineRequest(
                "CNC Mill", "SN-001", departmentId, null,
                null, null, null, null, commissioned, decommissioned,
                null, null, null
        );

        assertThatThrownBy(() -> machineService.createMachine(request))
                .isInstanceOf(InvalidMachineException.class);
    }

    @Test
    void rejectsNonPositiveMaintenanceInterval() {
        UUID departmentId = UUID.randomUUID();
        CreateMachineRequest request = new CreateMachineRequest(
                "CNC Mill", "SN-001", departmentId, null,
                null, null, null, null, null, null, null, null, 0
        );

        assertThatThrownBy(() -> machineService.createMachine(request))
                .isInstanceOf(InvalidMachineException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void translatesDuplicateSerialNumberToConflictException() {
        UUID departmentId = UUID.randomUUID();
        Department department = department(departmentId);
        MachineStatus operational = status(UUID.randomUUID(), "Operational", "#22C55E", true, "OPERATIONAL");
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(machineStatusRepository.findBySystemKey("OPERATIONAL")).thenReturn(Optional.of(operational));
        when(machineRepository.saveAndFlush(any(Machine.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate serial"));

        assertThatThrownBy(() -> machineService.createMachine(createRequest(departmentId, null)))
                .isInstanceOf(DuplicateMachineException.class);
    }

    @Test
    void updatesMachineAndCanAssignNewStatus() {
        UUID machineId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID newDepartmentId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        Machine machine = machine(machineId, department(departmentId),
                status(UUID.randomUUID(), "Operational", "#22C55E", true, "OPERATIONAL"));
        Department newDepartment = department(newDepartmentId);
        MachineStatus newStatus = status(newStatusId, "Awaiting parts", "#8B5CF6", false, null);
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));
        when(departmentRepository.findById(newDepartmentId)).thenReturn(Optional.of(newDepartment));
        when(machineStatusRepository.findById(newStatusId)).thenReturn(Optional.of(newStatus));
        when(machineRepository.saveAndFlush(machine)).thenReturn(machine);

        MachineResponse response = machineService.updateMachine(
                machineId,
                new UpdateMachineRequest(
                        "Research Mill", "SN-002", newDepartmentId, newStatusId,
                        "Mill", "Acme", "M-1", "Lab", null, null, null, null, 30
                )
        );

        assertThat(response.name()).isEqualTo("Research Mill");
        assertThat(response.serialNumber()).isEqualTo("SN-002");
        assertThat(response.departmentId()).isEqualTo(newDepartmentId);
        assertThat(response.statusId()).isEqualTo(newStatusId);
        assertThat(response.maintenanceIntervalDays()).isEqualTo(30);
    }

    @Test
    void archivesMachineByMarkingItDeleted() {
        UUID machineId = UUID.randomUUID();
        Machine machine = machine(machineId, department(UUID.randomUUID()),
                status(UUID.randomUUID(), "Operational", "#22C55E", true, "OPERATIONAL"));
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));

        machineService.archiveMachine(machineId);

        assertThat(machine.isDeleted()).isTrue();
        verify(machineRepository).save(machine);
        verify(machineRepository, never()).deleteById(any());
    }

    @Test
    void listsMachinesUsingRequestedFilters() {
        UUID departmentId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        Machine machine = machine(UUID.randomUUID(), department(departmentId),
                status(statusId, "Fault", "#EF4444", true, "FAULT"));
        when(machineRepository.findByDepartment_IdAndStatus_IdOrderByNameAsc(departmentId, statusId))
                .thenReturn(List.of(machine));

        List<MachineResponse> result = machineService.listMachines(departmentId, statusId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).statusName()).isEqualTo("Fault");
    }

    private CreateMachineRequest createRequest(UUID departmentId, UUID statusId) {
        return new CreateMachineRequest(
                "CNC Mill", "SN-001", departmentId, statusId,
                "Mill", "Acme", "M-1", "Plant 1",
                Instant.parse("2026-01-01T00:00:00Z"), null,
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z"), 90
        );
    }

    private Department department(UUID id) {
        Department department = new Department();
        department.setId(id);
        department.setDeptName("Operations");
        return department;
    }

    private MachineStatus status(UUID id, String name, String color, boolean builtIn, String systemKey) {
        MachineStatus status = new MachineStatus();
        status.setId(id);
        status.setName(name);
        status.setColor(color);
        status.setBuiltIn(builtIn);
        status.setSystemKey(systemKey);
        return status;
    }

    private Machine machine(UUID id, Department department, MachineStatus status) {
        Machine machine = new Machine();
        machine.setId(id);
        machine.setName("CNC Mill");
        machine.setSerialNumber("SN-001");
        machine.setDepartment(department);
        machine.setStatus(status);
        return machine;
    }
}
