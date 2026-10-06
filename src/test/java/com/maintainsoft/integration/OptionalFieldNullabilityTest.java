package com.maintainsoft.integration;

import com.maintainsoft.dto.CreateMachineRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.CreateUserRequest;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.service.MachineService;
import com.maintainsoft.service.RepairService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.service.UserManagementService;
import com.maintainsoft.testsupport.TestSecurityContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optional fields must be genuinely optional, and required ones must fail loudly.
 *
 * <p>A null reaching a {@code not null} column surfaces as a 500 from the persistence
 * layer, so these cases assert the request is refused before that happens rather than
 * being stored as corrupt data or reported as a server fault.
 */
@SpringBootTest
class OptionalFieldNullabilityTest {

    @Autowired
    private MachineService machineService;
    @Autowired
    private RepairService repairService;
    @Autowired
    private SpareService spareService;
    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManager entityManager;

    private UUID departmentId;
    private UUID machineId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Department department = new Department();
            department.setDeptName("Nullability-" + UUID.randomUUID());
            department.setPocName("POC");
            department.setPocNumber(4_400_000L);
            departmentRepository.saveAndFlush(department);
            departmentId = department.getId();
        });
        machineId = TestSecurityContext.runAsManager(() -> machineService.createMachine(
                machine("Base")).id());
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("delete from repair_spares where spare_id in "
                            + "(select id from spares where part_number like 'NULL-%')")
                    .executeUpdate();
            entityManager.createNativeQuery(
                            "delete from repairs where machine_id in "
                                    + "(select id from machines where department_id = :id)")
                    .setParameter("id", departmentId).executeUpdate();
            entityManager.createNativeQuery("delete from machines where department_id = :id")
                    .setParameter("id", departmentId).executeUpdate();
            entityManager.createNativeQuery("delete from spares where part_number like 'NULL-%'")
                    .executeUpdate();
            entityManager.createNativeQuery("delete from users where email like 'null-%@example.com'")
                    .executeUpdate();
            entityManager.createNativeQuery("delete from departments where id = :id")
                    .setParameter("id", departmentId).executeUpdate();
        });
    }

    @Test
    void everyOptionalMachineFieldMayBeOmitted() {
        var machine = TestSecurityContext.runAsManager(() -> machineService.createMachine(
                new CreateMachineRequest(
                        "No extras", "SN-" + UUID.randomUUID(), departmentId, null,
                        null, null, null, null, null, null,
                        null, null, null)));

        assertThat(machine.statusId()).isNotNull();
        assertThat(machine.equipmentType()).isNull();
        assertThat(machine.manufacturer()).isNull();
        assertThat(machine.maintenanceIntervalDays()).isNull();
    }

    @Test
    void everyOptionalRepairFieldMayBeOmitted() {
        var repair = TestSecurityContext.runAsManager(() -> repairService.createRepair(
                new CreateRepairRequest(
                        machineId, RepairType.BREAKDOWN, null,
                        "Minimal", "null-" + UUID.randomUUID(),
                        null, null, null),
                TestSecurityContext.asManager()));

        assertThat(repair.priority()).isNotNull();
        assertThat(repair.externalTechnicianName()).isNull();
        assertThat(repair.assignedSupervisorId()).isNull();
    }

    @Test
    void everyOptionalSpareFieldMayBeOmitted() {
        var spare = TestSecurityContext.runAsManager(() -> spareService.createSpare(
                new CreateSpareRequest("NULL-" + UUID.randomUUID(), "Bare spare",
                        null, null, null, 0)));

        assertThat(spare.partNumber()).isNotNull();
        assertThat(spare.description()).isNull();
        assertThat(spare.unit()).isNull();
        assertThat(spare.compatibleMachine()).isNull();
        assertThat(spare.stock()).isZero();
    }

    @Test
    void aSpareMayStartWithZeroStock() {
        var spare = TestSecurityContext.runAsManager(() -> spareService.createSpare(
                new CreateSpareRequest("NULL-" + UUID.randomUUID(), "Empty shelf",
                        null, null, null, 0)));

        assertThat(spare.stock()).isZero();
    }

    @Test
    void requiredRepairFieldsAreStillRequired() {
        assertThatThrownBy(() -> TestSecurityContext.runAsManager(
                () -> repairService.createRepair(
                        new CreateRepairRequest(null, RepairType.BREAKDOWN, null,
                                "No machine", "null-" + UUID.randomUUID(), null, null, null),
                        TestSecurityContext.asManager())))
                .isInstanceOf(com.maintainsoft.exception.InvalidRepairException.class);
    }

    @Test
    void requiredUserFieldsAreStillRequired() {
        assertThatThrownBy(() -> TestSecurityContext.runAsManager(
                () -> userManagementService.createSupervisor(new CreateUserRequest(
                        "No phone", "null-" + UUID.randomUUID() + "@example.com",
                        "password123", null, departmentId))))
                .isInstanceOf(RuntimeException.class);
    }

    private CreateMachineRequest machine(String prefix) {
        return new CreateMachineRequest(
                prefix, "SN-" + UUID.randomUUID(), departmentId, null,
                null, null, null, null, null, null,
                null, null, null);
    }
}