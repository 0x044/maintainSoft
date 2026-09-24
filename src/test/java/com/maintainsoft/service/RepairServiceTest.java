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
import com.maintainsoft.enums.RepairCostCategory;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.enums.Role;
import com.maintainsoft.exception.InvalidRepairException;
import com.maintainsoft.exception.RepairConflictException;
import com.maintainsoft.exception.RepairForbiddenException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.RepairCostRepository;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.RepairUpdateRepository;
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

import java.math.BigDecimal;
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

    @Mock
    private RepairUpdateRepository repairUpdateRepository;

    @Mock
    private RepairCostRepository repairCostRepository;

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

    @Test
    void managerAssignsRepairToSupervisor() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setRole(Role.SUPERVISOR);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(userRepository.findById(supervisor.getId())).thenReturn(Optional.of(supervisor));
        when(repairRepository.saveAndFlush(repair)).thenReturn(repair);

        RepairResponse response = repairService.assignRepair(
                repair.getId(),
                new AssignRepairRequest(supervisor.getId()),
                authentication("manager@example.com", "ROLE_MANAGER")
        );

        assertThat(response.assignedSupervisorId()).isEqualTo(supervisor.getId());
    }

    @Test
    void managerCanTemporarilyUnassignRepair() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setId(UUID.randomUUID());
        supervisor.setRole(Role.SUPERVISOR);
        repair.setAssignedSupervisor(supervisor);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(repairRepository.saveAndFlush(repair)).thenReturn(repair);

        RepairResponse response = repairService.assignRepair(
                repair.getId(),
                new AssignRepairRequest(null),
                authentication("manager@example.com", "ROLE_MANAGER")
        );

        assertThat(response.assignedSupervisorId()).isNull();
    }

    @Test
    void supervisorClaimsUnassignedRepair() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setRole(Role.SUPERVISOR);
        when(userRepository.findByEmail("supervisor@example.com")).thenReturn(Optional.of(supervisor));
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(repairRepository.saveAndFlush(repair)).thenReturn(repair);

        RepairResponse response = repairService.claimRepair(
                repair.getId(),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        );

        assertThat(response.assignedSupervisorId()).isEqualTo(supervisor.getId());
    }

    @Test
    void rejectsClaimWhenRepairIsAssignedToAnotherSupervisor() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User other = user("other@example.com", "Other");
        other.setRole(Role.SUPERVISOR);
        repair.setAssignedSupervisor(other);
        User currentSupervisor = user("supervisor@example.com", "Ravi");
        currentSupervisor.setRole(Role.SUPERVISOR);
        when(userRepository.findByEmail("supervisor@example.com"))
                .thenReturn(Optional.of(currentSupervisor));
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));

        assertThatThrownBy(() -> repairService.claimRepair(
                repair.getId(),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        )).isInstanceOf(RepairConflictException.class);
    }

    @Test
    void updatesRepairMasterData() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(repairRepository.saveAndFlush(repair)).thenReturn(repair);
        Instant newStart = Instant.parse("2026-09-25T04:00:00Z");

        RepairResponse response = repairService.updateRepair(
                repair.getId(),
                new UpdateRepairRequest(
                        "Updated description",
                        RepairPriority.LOW,
                        "New Technician",
                        "+91-9111111111",
                        newStart
                ),
                authentication("manager@example.com", "ROLE_MANAGER")
        );

        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.priority()).isEqualTo(RepairPriority.LOW);
        assertThat(response.externalTechnicianName()).isEqualTo("New Technician");
        assertThat(response.startDate()).isEqualTo(newStart);
    }

    @Test
    void rejectsRepairManagementForUnprivilegedUser() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);

        assertThatThrownBy(() -> repairService.updateRepair(
                repair.getId(),
                new UpdateRepairRequest("Changed", null, null, null, null),
                authentication("reporter@example.com", "ROLE_REPORTER")
        )).isInstanceOf(RepairForbiddenException.class);

        verify(repairRepository, never()).findById(any());
    }

    @Test
    void assignedSupervisorCanAppendForwardStatusUpdate() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setRole(Role.SUPERVISOR);
        repair.setAssignedSupervisor(supervisor);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(userRepository.findByEmail("supervisor@example.com")).thenReturn(Optional.of(supervisor));
        when(repairRepository.save(repair)).thenReturn(repair);
        when(repairUpdateRepository.save(any(RepairUpdate.class))).thenAnswer(invocation -> {
            RepairUpdate update = invocation.getArgument(0);
            update.setId(UUID.randomUUID());
            return update;
        });

        RepairUpdateResponse response = repairService.addRepairUpdate(
                repair.getId(),
                new AddRepairUpdateRequest(RepairStatus.IN_PROGRESS, "Technician started work"),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        );

        assertThat(repair.getRepairStatus()).isEqualTo(RepairStatus.IN_PROGRESS);
        assertThat(response.status()).isEqualTo(RepairStatus.IN_PROGRESS);
        assertThat(response.repairId()).isEqualTo(repair.getId());
    }

    @Test
    void managerCanAppendInrCostToUnassignedRepair() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(repairCostRepository.save(any(RepairCost.class))).thenAnswer(invocation -> {
            RepairCost cost = invocation.getArgument(0);
            cost.setId(UUID.randomUUID());
            return cost;
        });

        RepairCostResponse response = repairService.addRepairCost(
                repair.getId(),
                new AddRepairCostRequest(
                        RepairCostCategory.LABOR,
                        new BigDecimal("1250.5"),
                        "Technician labor"
                ),
                authentication("manager@example.com", "ROLE_MANAGER")
        );

        assertThat(response.amount()).isEqualByComparingTo("1250.50");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.category()).isEqualTo(RepairCostCategory.LABOR);
    }

    @Test
    void rejectsSupervisorUpdateForUnassignedRepair() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setRole(Role.SUPERVISOR);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(userRepository.findByEmail("supervisor@example.com")).thenReturn(Optional.of(supervisor));

        assertThatThrownBy(() -> repairService.addRepairUpdate(
                repair.getId(),
                new AddRepairUpdateRequest(RepairStatus.IN_PROGRESS, "Started"),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        )).isInstanceOf(RepairForbiddenException.class);

        verify(repairUpdateRepository, never()).save(any());
    }

    @Test
    void rejectsSkippingRepairStatus() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        User supervisor = user("supervisor@example.com", "Ravi");
        supervisor.setRole(Role.SUPERVISOR);
        repair.setAssignedSupervisor(supervisor);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));
        when(userRepository.findByEmail("supervisor@example.com")).thenReturn(Optional.of(supervisor));

        assertThatThrownBy(() -> repairService.addRepairUpdate(
                repair.getId(),
                new AddRepairUpdateRequest(RepairStatus.COMPLETED, "Finished"),
                authentication("supervisor@example.com", "ROLE_SUPERVISOR")
        )).isInstanceOf(InvalidRepairException.class);

        verify(repairRepository, never()).save(any());
        verify(repairUpdateRepository, never()).save(any());
    }

    @Test
    void rejectsCostWithMoreThanTwoDecimalPlaces() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));

        assertThatThrownBy(() -> repairService.addRepairCost(
                repair.getId(),
                new AddRepairCostRequest(
                        RepairCostCategory.OTHER,
                        new BigDecimal("10.123"),
                        "Invalid precision"
                ),
                authentication("manager@example.com", "ROLE_MANAGER")
        )).isInstanceOf(InvalidRepairException.class);

        verify(repairCostRepository, never()).save(any());
    }

    @Test
    void listsAppendOnlyUpdateAndCostHistory() {
        Repair repair = repair(machine(), RepairType.BREAKDOWN);
        when(repairRepository.findById(repair.getId())).thenReturn(Optional.of(repair));

        RepairUpdate update = new RepairUpdate();
        update.setId(UUID.randomUUID());
        update.setRepair(repair);
        update.setRepairStatus(RepairStatus.IN_PROGRESS);
        update.setDescription("Started");
        when(repairUpdateRepository.findByRepair_IdOrderByCreatedAtAsc(repair.getId()))
                .thenReturn(List.of(update));

        RepairCost cost = new RepairCost();
        cost.setId(UUID.randomUUID());
        cost.setRepair(repair);
        cost.setCategory(RepairCostCategory.PARTS);
        cost.setAmount(new BigDecimal("10.00"));
        when(repairCostRepository.findByRepair_IdOrderByCreatedAtAsc(repair.getId()))
                .thenReturn(List.of(cost));

        assertThat(repairService.listRepairUpdates(repair.getId())).hasSize(1);
        assertThat(repairService.listRepairCosts(repair.getId())).hasSize(1);
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
