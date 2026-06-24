package com.maintainsoft.entity;

import com.maintainsoft.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTest {

    // ──────────────────────────────────────────────
    //  BaseEntity
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("BaseEntity")
    class BaseEntityTests {

        @Test
        @DisplayName("should get and set id (UUID)")
        void getSetId() {
            // BaseEntity is abstract (@MappedSuperclass) so we test via a concrete subclass
            Department entity = new Department();
            UUID id = UUID.randomUUID();

            entity.setId(id);

            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("should get and set createdAt")
        void getSetCreatedAt() {
            Department entity = new Department();
            Instant now = Instant.now();

            entity.setCreatedAt(now);

            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should get and set updatedAt")
        void getSetUpdatedAt() {
            Department entity = new Department();
            Instant now = Instant.now();

            entity.setUpdatedAt(now);

            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should get and set createdBy")
        void getSetCreatedBy() {
            Department entity = new Department();

            entity.setCreatedBy("admin@test.com");

            assertThat(entity.getCreatedBy()).isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("should get and set updatedBy")
        void getSetUpdatedBy() {
            Department entity = new Department();

            entity.setUpdatedBy("editor@test.com");

            assertThat(entity.getUpdatedBy()).isEqualTo("editor@test.com");
        }

        @Test
        @DisplayName("all audit fields should be null by default")
        void defaultsAreNull() {
            Department entity = new Department();

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getCreatedBy()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    //  Department
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Department")
    class DepartmentTests {

        @Test
        @DisplayName("should get and set deptName")
        void getSetDeptName() {
            Department dept = new Department();
            dept.setDeptName("Engineering");

            assertThat(dept.getDeptName()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("should get and set pocName")
        void getSetPocName() {
            Department dept = new Department();
            dept.setPocName("John Doe");

            assertThat(dept.getPocName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should get and set pocNumber")
        void getSetPocNumber() {
            Department dept = new Department();
            dept.setPocNumber(9876543210L);

            assertThat(dept.getPocNumber()).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("should get and set version")
        void getSetVersion() {
            Department dept = new Department();
            dept.setVersion(1L);

            assertThat(dept.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deleted should default to false")
        void deletedDefaultsFalse() {
            Department dept = new Department();

            assertThat(dept.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("should be able to set deleted to true")
        void canSetDeletedTrue() {
            Department dept = new Department();
            dept.setDeleted(true);

            assertThat(dept.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("machineList should default to empty ArrayList")
        void machineListDefaultsToEmptyList() {
            Department dept = new Department();

            assertThat(dept.getMachineList()).isNotNull();
            assertThat(dept.getMachineList()).isEmpty();
            assertThat(dept.getMachineList()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("should be able to set machineList")
        void canSetMachineList() {
            Department dept = new Department();
            Machine machine = new Machine();
            machine.setName("CNC Lathe");
            List<Machine> machines = new ArrayList<>();
            machines.add(machine);

            dept.setMachineList(machines);

            assertThat(dept.getMachineList()).hasSize(1);
            assertThat(dept.getMachineList().get(0).getName()).isEqualTo("CNC Lathe");
        }

        @Test
        @DisplayName("should inherit BaseEntity fields")
        void inheritsBaseEntity() {
            Department dept = new Department();
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            dept.setId(id);
            dept.setCreatedAt(now);
            dept.setCreatedBy("admin@test.com");

            assertThat(dept.getId()).isEqualTo(id);
            assertThat(dept.getCreatedAt()).isEqualTo(now);
            assertThat(dept.getCreatedBy()).isEqualTo("admin@test.com");
        }
    }

    // ──────────────────────────────────────────────
    //  Machine
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Machine")
    class MachineTests {

        @Test
        @DisplayName("should get and set name")
        void getSetName() {
            Machine machine = new Machine();
            machine.setName("CNC Mill");

            assertThat(machine.getName()).isEqualTo("CNC Mill");
        }

        @Test
        @DisplayName("should get and set department")
        void getSetDepartment() {
            Machine machine = new Machine();
            Department dept = new Department();
            dept.setDeptName("Manufacturing");

            machine.setDepartment(dept);

            assertThat(machine.getDepartment()).isNotNull();
            assertThat(machine.getDepartment().getDeptName()).isEqualTo("Manufacturing");
        }

        @Test
        @DisplayName("should get and set serialNumber")
        void getSetSerialNumber() {
            Machine machine = new Machine();
            machine.setSerialNumber("SN-001-ABC");

            assertThat(machine.getSerialNumber()).isEqualTo("SN-001-ABC");
        }

        @Test
        @DisplayName("status should default to OPERATIONAL")
        void statusDefaultsToOperational() {
            Machine machine = new Machine();

            assertThat(machine.getStatus()).isEqualTo(OperationCondition.OPERATIONAL);
        }

        @Test
        @DisplayName("should be able to change status")
        void canChangeStatus() {
            Machine machine = new Machine();
            machine.setStatus(OperationCondition.UNDER_MAINTENANCE);

            assertThat(machine.getStatus()).isEqualTo(OperationCondition.UNDER_MAINTENANCE);
        }

        @Test
        @DisplayName("deleted should default to false")
        void deletedDefaultsFalse() {
            Machine machine = new Machine();

            assertThat(machine.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("should get and set version")
        void getSetVersion() {
            Machine machine = new Machine();
            machine.setVersion(5L);

            assertThat(machine.getVersion()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should support all-args constructor")
        void allArgsConstructor() {
            Machine machine = new Machine("Drill Press", null, "SN-002",
                    OperationCondition.DECOMMISSIONED, true, 3L);

            assertThat(machine.getName()).isEqualTo("Drill Press");
            assertThat(machine.getSerialNumber()).isEqualTo("SN-002");
            assertThat(machine.getStatus()).isEqualTo(OperationCondition.DECOMMISSIONED);
            assertThat(machine.isDeleted()).isTrue();
            assertThat(machine.getVersion()).isEqualTo(3L);
        }
    }

    // ──────────────────────────────────────────────
    //  Repair
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Repair")
    class RepairTests {

        @Test
        @DisplayName("should get and set machine")
        void getSetMachine() {
            Repair repair = new Repair();
            Machine machine = new Machine();
            machine.setName("Conveyor Belt");

            repair.setMachine(machine);

            assertThat(repair.getMachine()).isNotNull();
            assertThat(repair.getMachine().getName()).isEqualTo("Conveyor Belt");
        }

        @Test
        @DisplayName("should get and set technician")
        void getSetTechnician() {
            Repair repair = new Repair();
            Technician tech = new Technician();
            tech.setName("Alice");

            repair.setTechnician(tech);

            assertThat(repair.getTechnician().getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("should get and set repairStatus")
        void getSetRepairStatus() {
            Repair repair = new Repair();
            repair.setRepairStatus(RepairStatus.IN_PROGRESS);

            assertThat(repair.getRepairStatus()).isEqualTo(RepairStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should get and set repairType")
        void getSetRepairType() {
            Repair repair = new Repair();
            repair.setRepairType(RepairType.PREVENTIVE);

            assertThat(repair.getRepairType()).isEqualTo(RepairType.PREVENTIVE);
        }

        @Test
        @DisplayName("repairPriority should default to NORMAL")
        void repairPriorityDefaultsToNormal() {
            Repair repair = new Repair();

            assertThat(repair.getRepairPriority()).isEqualTo(RepairPriority.NORMAL);
        }

        @Test
        @DisplayName("should be able to change repairPriority")
        void canChangeRepairPriority() {
            Repair repair = new Repair();
            repair.setRepairPriority(RepairPriority.HIGH);

            assertThat(repair.getRepairPriority()).isEqualTo(RepairPriority.HIGH);
        }

        @Test
        @DisplayName("should get and set description")
        void getSetDescription() {
            Repair repair = new Repair();
            repair.setDescription("Bearing replacement needed");

            assertThat(repair.getDescription()).isEqualTo("Bearing replacement needed");
        }

        @Test
        @DisplayName("should get and set startDate and endDate")
        void getSetDates() {
            Repair repair = new Repair();
            Instant start = Instant.parse("2026-01-01T10:00:00Z");
            Instant end = Instant.parse("2026-01-02T10:00:00Z");

            repair.setStartDate(start);
            repair.setEndDate(end);

            assertThat(repair.getStartDate()).isEqualTo(start);
            assertThat(repair.getEndDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("should get and set idempotencyKey")
        void getSetIdempotencyKey() {
            Repair repair = new Repair();
            repair.setIdempotencyKey("key-abc-123");

            assertThat(repair.getIdempotencyKey()).isEqualTo("key-abc-123");
        }

        @Test
        @DisplayName("updateList should default to empty ArrayList")
        void updateListDefaultsToEmptyList() {
            Repair repair = new Repair();

            assertThat(repair.getUpdateList()).isNotNull();
            assertThat(repair.getUpdateList()).isEmpty();
        }

        @Test
        @DisplayName("sparesList should default to empty ArrayList")
        void sparesListDefaultsToEmptyList() {
            Repair repair = new Repair();

            assertThat(repair.getSparesList()).isNotNull();
            assertThat(repair.getSparesList()).isEmpty();
        }

        @Test
        @DisplayName("should get and set version")
        void getSetVersion() {
            Repair repair = new Repair();
            repair.setVersion(2L);

            assertThat(repair.getVersion()).isEqualTo(2L);
        }
    }

    // ──────────────────────────────────────────────
    //  RepairSpare + RepairSpareId
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("RepairSpare")
    class RepairSpareTests {

        @Test
        @DisplayName("should have default RepairSpareId initialized")
        void defaultIdIsInitialized() {
            RepairSpare rs = new RepairSpare();

            assertThat(rs.getId()).isNotNull();
            assertThat(rs.getId()).isInstanceOf(RepairSpare.RepairSpareId.class);
        }

        @Test
        @DisplayName("should get and set repair")
        void getSetRepair() {
            RepairSpare rs = new RepairSpare();
            Repair repair = new Repair();
            repair.setDescription("Fix motor");

            rs.setRepair(repair);

            assertThat(rs.getRepair()).isNotNull();
            assertThat(rs.getRepair().getDescription()).isEqualTo("Fix motor");
        }

        @Test
        @DisplayName("should get and set spare")
        void getSetSpare() {
            RepairSpare rs = new RepairSpare();
            Spare spare = new Spare();
            spare.setName("Bolt M10");

            rs.setSpare(spare);

            assertThat(rs.getSpare()).isNotNull();
            assertThat(rs.getSpare().getName()).isEqualTo("Bolt M10");
        }

        @Test
        @DisplayName("should get and set usedQuantity")
        void getSetUsedQuantity() {
            RepairSpare rs = new RepairSpare();
            rs.setUsedQuantity(15);

            assertThat(rs.getUsedQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("usedQuantity should default to 0")
        void usedQuantityDefaultsToZero() {
            RepairSpare rs = new RepairSpare();

            assertThat(rs.getUsedQuantity()).isZero();
        }

        @Test
        @DisplayName("RepairSpareId should get and set repairId")
        void repairSpareIdGetSetRepairId() {
            RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();
            UUID repairId = UUID.randomUUID();

            id.setRepairId(repairId);

            assertThat(id.getRepairId()).isEqualTo(repairId);
        }

        @Test
        @DisplayName("RepairSpareId should get and set spareId")
        void repairSpareIdGetSetSpareId() {
            RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();
            UUID spareId = UUID.randomUUID();

            id.setSpareId(spareId);

            assertThat(id.getSpareId()).isEqualTo(spareId);
        }

        @Test
        @DisplayName("RepairSpareId fields should be null by default")
        void repairSpareIdDefaultsNull() {
            RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();

            assertThat(id.getRepairId()).isNull();
            assertThat(id.getSpareId()).isNull();
        }

        @Test
        @DisplayName("should be able to set embedded id on RepairSpare")
        void canSetEmbeddedId() {
            RepairSpare rs = new RepairSpare();
            RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();
            UUID repairId = UUID.randomUUID();
            UUID spareId = UUID.randomUUID();
            id.setRepairId(repairId);
            id.setSpareId(spareId);

            rs.setId(id);

            assertThat(rs.getId().getRepairId()).isEqualTo(repairId);
            assertThat(rs.getId().getSpareId()).isEqualTo(spareId);
        }
    }

    // ──────────────────────────────────────────────
    //  RepairUpdate
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("RepairUpdate")
    class RepairUpdateTests {

        @Test
        @DisplayName("should get and set repair")
        void getSetRepair() {
            RepairUpdate update = new RepairUpdate();
            Repair repair = new Repair();

            update.setRepair(repair);

            assertThat(update.getRepair()).isSameAs(repair);
        }

        @Test
        @DisplayName("should get and set description")
        void getSetDescription() {
            RepairUpdate update = new RepairUpdate();
            update.setDescription("Parts ordered, awaiting delivery");

            assertThat(update.getDescription()).isEqualTo("Parts ordered, awaiting delivery");
        }

        @Test
        @DisplayName("should get and set repairStatus")
        void getSetRepairStatus() {
            RepairUpdate update = new RepairUpdate();
            update.setRepairStatus(RepairStatus.COMPLETED);

            assertThat(update.getRepairStatus()).isEqualTo(RepairStatus.COMPLETED);
        }

        @Test
        @DisplayName("should inherit BaseEntity fields")
        void inheritsBaseEntity() {
            RepairUpdate update = new RepairUpdate();
            UUID id = UUID.randomUUID();
            update.setId(id);

            assertThat(update.getId()).isEqualTo(id);
        }
    }

    // ──────────────────────────────────────────────
    //  Spare
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Spare")
    class SpareTests {

        @Test
        @DisplayName("should get and set partNumber")
        void getSetPartNumber() {
            Spare spare = new Spare();
            spare.setPartNumber("PN-12345");

            assertThat(spare.getPartNumber()).isEqualTo("PN-12345");
        }

        @Test
        @DisplayName("should get and set name")
        void getSetName() {
            Spare spare = new Spare();
            spare.setName("Hydraulic Pump");

            assertThat(spare.getName()).isEqualTo("Hydraulic Pump");
        }

        @Test
        @DisplayName("should get and set cost with BigDecimal")
        void getSetCost() {
            Spare spare = new Spare();
            BigDecimal cost = new BigDecimal("1234.5678");

            spare.setCost(cost);

            assertThat(spare.getCost()).isEqualByComparingTo(cost);
        }

        @Test
        @DisplayName("should get and set lastPurchaseDate")
        void getSetLastPurchaseDate() {
            Spare spare = new Spare();
            Instant date = Instant.parse("2026-06-01T12:00:00Z");

            spare.setLastPurchaseDate(date);

            assertThat(spare.getLastPurchaseDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("should get and set stock")
        void getSetStock() {
            Spare spare = new Spare();
            spare.setStock(100);

            assertThat(spare.getStock()).isEqualTo(100);
        }

        @Test
        @DisplayName("stock should default to 0")
        void stockDefaultsToZero() {
            Spare spare = new Spare();

            assertThat(spare.getStock()).isZero();
        }

        @Test
        @DisplayName("deleted should default to false")
        void deletedDefaultsFalse() {
            Spare spare = new Spare();

            assertThat(spare.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("should be able to set deleted to true")
        void canSetDeletedTrue() {
            Spare spare = new Spare();
            spare.setDeleted(true);

            assertThat(spare.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("should get and set version")
        void getSetVersion() {
            Spare spare = new Spare();
            spare.setVersion(7L);

            assertThat(spare.getVersion()).isEqualTo(7L);
        }
    }

    // ──────────────────────────────────────────────
    //  Technician
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Technician")
    class TechnicianTests {

        @Test
        @DisplayName("should get and set name")
        void getSetName() {
            Technician tech = new Technician();
            tech.setName("Bob Smith");

            assertThat(tech.getName()).isEqualTo("Bob Smith");
        }

        @Test
        @DisplayName("should get and set phone")
        void getSetPhone() {
            Technician tech = new Technician();
            tech.setPhone("555-1234");

            assertThat(tech.getPhone()).isEqualTo("555-1234");
        }

        @Test
        @DisplayName("should inherit BaseEntity fields")
        void inheritsBaseEntity() {
            Technician tech = new Technician();
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            tech.setId(id);
            tech.setCreatedAt(now);
            tech.setUpdatedAt(now);
            tech.setCreatedBy("system");
            tech.setUpdatedBy("system");

            assertThat(tech.getId()).isEqualTo(id);
            assertThat(tech.getCreatedAt()).isEqualTo(now);
            assertThat(tech.getUpdatedAt()).isEqualTo(now);
            assertThat(tech.getCreatedBy()).isEqualTo("system");
            assertThat(tech.getUpdatedBy()).isEqualTo("system");
        }
    }

    // ──────────────────────────────────────────────
    //  User
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("User")
    class UserTests {

        @Test
        @DisplayName("should get and set name")
        void getSetName() {
            User user = new User();
            user.setName("Jane Doe");

            assertThat(user.getName()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("should get and set password")
        void getSetPassword() {
            User user = new User();
            user.setPassword("$2a$10$hashedpassword");

            assertThat(user.getPassword()).isEqualTo("$2a$10$hashedpassword");
        }

        @Test
        @DisplayName("should get and set email")
        void getSetEmail() {
            User user = new User();
            user.setEmail("jane@example.com");

            assertThat(user.getEmail()).isEqualTo("jane@example.com");
        }

        @Test
        @DisplayName("should get and set role")
        void getSetRole() {
            User user = new User();
            user.setRole(Role.MANAGER);

            assertThat(user.getRole()).isEqualTo(Role.MANAGER);
        }

        @Test
        @DisplayName("should get and set phone")
        void getSetPhone() {
            User user = new User();
            user.setPhone("555-9876");

            assertThat(user.getPhone()).isEqualTo("555-9876");
        }

        @Test
        @DisplayName("deleted should default to false")
        void deletedDefaultsFalse() {
            User user = new User();

            assertThat(user.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("should be able to set deleted to true")
        void canSetDeletedTrue() {
            User user = new User();
            user.setDeleted(true);

            assertThat(user.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("should get and set department")
        void getSetDepartment() {
            User user = new User();
            Department dept = new Department();
            dept.setDeptName("IT");

            user.setDepartment(dept);

            assertThat(user.getDepartment()).isNotNull();
            assertThat(user.getDepartment().getDeptName()).isEqualTo("IT");
        }

        @Test
        @DisplayName("should get and set version")
        void getSetVersion() {
            User user = new User();
            user.setVersion(3L);

            assertThat(user.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should support all-args constructor")
        void allArgsConstructor() {
            Department dept = new Department();
            User user = new User("Test User", "pass", "test@test.com",
                    Role.SUPERVISOR, "555-0000", false, dept, 1L);

            assertThat(user.getName()).isEqualTo("Test User");
            assertThat(user.getPassword()).isEqualTo("pass");
            assertThat(user.getEmail()).isEqualTo("test@test.com");
            assertThat(user.getRole()).isEqualTo(Role.SUPERVISOR);
            assertThat(user.getPhone()).isEqualTo("555-0000");
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getDepartment()).isSameAs(dept);
            assertThat(user.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should support no-args constructor")
        void noArgsConstructor() {
            User user = new User();

            assertThat(user.getName()).isNull();
            assertThat(user.getEmail()).isNull();
            assertThat(user.getRole()).isNull();
        }
    }
}
