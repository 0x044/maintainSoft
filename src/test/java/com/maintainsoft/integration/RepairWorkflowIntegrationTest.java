package com.maintainsoft.integration;

import com.maintainsoft.dto.AddRepairCostRequest;
import com.maintainsoft.dto.AddRepairUpdateRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.RepairCostResponse;
import com.maintainsoft.dto.RepairResponse;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.StockIssueRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.MachineStatus;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.RepairSpare;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.enums.RepairCostCategory;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.MachineRepository;
import com.maintainsoft.repository.MachineStatusRepository;
import com.maintainsoft.repository.RepairCostRepository;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.RepairSpareRepository;
import com.maintainsoft.repository.SpareRepository;
import com.maintainsoft.service.RepairService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.testsupport.TestSecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepairWorkflowIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private MachineStatusRepository machineStatusRepository;

    @Autowired
    private SpareRepository spareRepository;

    @Autowired
    private RepairRepository repairRepository;

    @Autowired
    private RepairSpareRepository repairSpareRepository;

    @Autowired
    private RepairCostRepository repairCostRepository;

    @Autowired
    private SpareService spareService;

    @Autowired
    private RepairService repairService;

    @Test
    void completesBreakdownStockAndRepairCostFlowInOneTransaction() {
        String suffix = UUID.randomUUID().toString();
        Department department = new Department();
        department.setDeptName("Integration-" + suffix);
        department.setPocName("Integration POC");
        department.setPocNumber(9_000_000L + Math.abs(suffix.hashCode() % 100_000));
        departmentRepository.saveAndFlush(department);

        MachineStatus operational = machineStatusRepository.findBySystemKey("OPERATIONAL")
                .orElseThrow();
        Machine machine = new Machine();
        machine.setName("Integration Machine");
        machine.setSerialNumber("INT-" + suffix);
        machine.setDepartment(department);
        machine.setStatus(operational);
        machineRepository.saveAndFlush(machine);

        Spare spare = new Spare();
        spare.setPartNumber("INT-" + suffix);
        spare.setName("Integration Bearing");
        spare.setStock(3);
        spareRepository.saveAndFlush(spare);

        RepairResponse created = repairService.createRepair(
                new CreateRepairRequest(
                        machine.getId(),
                        RepairType.BREAKDOWN,
                        null,
                        "Integration breakdown",
                        "integration-" + suffix,
                        "External Technician",
                        "+91-9000000000",
                        Instant.now()
                ),
                authentication("ROLE_REPORTER")
        );

        // Stock operations are authorized from the security context rather than an
        // argument, and issuing stock is a manager/supervisor operation. The caller has
        // to be presented the way a request would.
        SpareResponse[] afterIssue = new SpareResponse[1];
        TestSecurityContext.runAs(authentication("ROLE_MANAGER"), () -> afterIssue[0] =
                spareService.issueStock(
                        spare.getId(),
                        new StockIssueRequest(2, created.id())
                ));
        assertThat(afterIssue[0].stock()).isEqualTo(1);

        var update = repairService.addRepairUpdate(
                created.id(),
                new AddRepairUpdateRequest(RepairStatus.IN_PROGRESS, "Technician started"),
                authentication("ROLE_MANAGER")
        );
        assertThat(update.status()).isEqualTo(RepairStatus.IN_PROGRESS);

        RepairCostResponse cost = repairService.addRepairCost(
                created.id(),
                new AddRepairCostRequest(
                        RepairCostCategory.LABOR,
                        new BigDecimal("1250.50"),
                        "Integration labor"
                ),
                authentication("ROLE_MANAGER")
        );
        assertThat(cost.amount()).isEqualByComparingTo("1250.50");
        assertThat(cost.currency()).isEqualTo("INR");

        Repair repair = repairRepository.findById(created.id()).orElseThrow();
        assertThat(repair.getRepairStatus()).isEqualTo(RepairStatus.IN_PROGRESS);
        assertThat(repair.getMachine().getStatus().getSystemKey()).isEqualTo("UNDER_MAINTENANCE");

        RepairSpare.RepairSpareId linkId = new RepairSpare.RepairSpareId();
        linkId.setRepairId(created.id());
        linkId.setSpareId(spare.getId());
        assertThat(repairSpareRepository.findById(linkId).orElseThrow().getUsedQuantity())
                .isEqualTo(2);
        assertThat(repairCostRepository.findByRepair_IdOrderByCreatedAtAsc(created.id()))
                .hasSize(1);
    }

    private Authentication authentication(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "integration@example.com",
                "ignored",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
