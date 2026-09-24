package com.maintainsoft.service;

import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.exception.RepairForbiddenException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
class RepairServiceTest {

    @Mock
    private RepairRepository repairRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RepairService repairService;

    @Test
    void createsBreakdownForAnyAuthenticatedUser() {
        Machine machine = machine();
        when(machineRepository.findById(machine.getId())).thenReturn(Optional.of(machine));
        when(repairRepository.saveAndFlush(any(Repair.class))).thenAnswer(invocation -> {
            Repair repair = invocation.getArgument(0);
            repair.setId(UUID.randomUUID());
            return repair;
        });

        RepairResponse response = repairService.createRepair(
                request(machine.getId(), RepairType.BREAKDOWN, "breakdown-1"),
                authentication("reporter@example.com", "ROLE_REPORTER")
        );

        assertThat(response.status()).isEqualTo(RepairStatus.OPEN);
        assertThat(response.type()).isEqualTo(RepairType.BREAKDOWN);
        assertThat(response.priority()).isEqualTo(RepairPriority.NORMAL);
        assertThat(response.assignedSupervisorId()).isNull();
        assertThat(response.startDate()).isNotNull();
    }

    @Test
    void createsScheduledRepairAndSelfAssignsSupervisor() {
        Machine machine = machine();
        User supervisor = user("supervisor@example.com", "Ravi");
        when(machineRepository.findById(machine.getId())).thenReturn(Optional.of(machine));
        when(userRepository.findByEmail("supervisor@example.com")).thenReturn(Optional.of(supervisor));
        when(repairRepository.saveAndFlush(any(Repair.class))).thenAnswer(invocation -> {
            Repair repair = invocation.getArgument(0);
            repair.setId(UUID.randomUUID());
            return repair;
        });

        RepairResponse response = repairService.createRepair(
                request(machine.getId(), RepairType.SCHEDULED, "scheduled-1"),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        );

        assertThat(response.assignedSupervisorId()).isEqualTo(supervisor.getId());
        assertThat(response.assignedSupervisorName()).isEqualTo("Ravi");
    }

    @Test
    void returnsExistingRepairForRepeatedIdempotencyKey() {
        Repair existing = repair(machine(), RepairType.BREAKDOWN);
        existing.setIdempotencyKey("breakdown-1");
        when(repairRepository.findByIdempotencyKey("breakdown-1")).thenReturn(Optional.of(existing));

        RepairResponse response = repairService.createRepair(
                request(existing.getMachine().getId(), RepairType.BREAKDOWN, "breakdown-1"),
                authentication("reporter@example.com", "ROLE_REPORTER")
        );

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(machineRepository, never()).findById(any());
        verify(repairRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsScheduledRepairForUnprivilegedAuthenticatedUser() {
        CreateRepairRequest request = request(UUID.randomUUID(), RepairType.SCHEDULED, "scheduled-2");

        assertThatThrownBy(() -> repairService.createRepair(
                request,
                authentication("reporter@example.com", "ROLE_REPORTER")
        )).isInstanceOf(RepairForbiddenException.class);

        verify(repairRepository, never()).findByIdempotencyKey(any());
    }

    @Test
    void rejectsRepairForUnknownMachine() {
        UUID machineId = UUID.randomUUID();
        when(repairRepository.findByIdempotencyKey("breakdown-2")).thenReturn(Optional.empty());
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repairService.createRepair(
                request(machineId, RepairType.BREAKDOWN, "breakdown-2"),
                authentication("reporter@example.com", "ROLE_REPORTER")
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsRepairsUsingCombinedFilters() {
        Machine machine = machine();
        Repair repair = repair(machine, RepairType.BREAKDOWN);
        when(repairRepository.findByRepairStatusAndMachine_IdOrderByCreatedAtDesc(
                RepairStatus.OPEN, machine.getId()
        )).thenReturn(List.of(repair));

        List<RepairResponse> result = repairService.listRepairs(RepairStatus.OPEN, machine.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(repair.getId());
    }

    @Test
    void translatesConcurrentIdempotencyRaceToConflict() {
        Machine machine = machine();
        when(machineRepository.findById(machine.getId())).thenReturn(Optional.of(machine));
        when(repairRepository.saveAndFlush(any(Repair.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> repairService.createRepair(
                request(machine.getId(), RepairType.BREAKDOWN, "breakdown-3"),
                authentication("reporter@example.com", "ROLE_REPORTER")
        )).isInstanceOf(com.maintainsoft.exception.DuplicateRepairException.class);
    }

    private CreateRepairRequest request(UUID machineId, RepairType type, String key) {
        return new CreateRepairRequest(
                machineId,
                type,
                type == RepairType.BREAKDOWN ? null : RepairPriority.HIGH,
                "Replace worn bearing",
                key,
                "External Technician",
                "+91-9000000000",
                Instant.parse("2026-09-24T04:00:00Z")
        );
    }

    private Authentication authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(
                email,
                "ignored",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    private Machine machine() {
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("CNC Mill");
        return machine;
    }

    private User user(String email, String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName(name);
        return user;
    }

    private Repair repair(Machine machine, RepairType type) {
        Repair repair = new Repair();
        repair.setId(UUID.randomUUID());
        repair.setMachine(machine);
        repair.setRepairType(type);
        repair.setRepairPriority(RepairPriority.NORMAL);
        repair.setRepairStatus(RepairStatus.OPEN);
        repair.setDescription("Description");
        repair.setStartDate(Instant.parse("2026-09-24T04:00:00Z"));
        return repair;
    }
}
