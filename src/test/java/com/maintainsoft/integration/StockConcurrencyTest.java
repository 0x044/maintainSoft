package com.maintainsoft.integration;

import com.maintainsoft.dto.CreateMachineRequest;
import com.maintainsoft.dto.CreateRepairRequest;
import com.maintainsoft.dto.StockIssueRequest;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Machine;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairType;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.SpareRepository;
import com.maintainsoft.service.MachineService;
import com.maintainsoft.service.RepairService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.testsupport.TestSecurityContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inventory has to stay correct when two requests touch the same spare at once.
 *
 * <p>These tests deliberately run without a rollback-only test transaction: every worker
 * commits in its own transaction, which is what makes the race real. Rows are removed
 * explicitly afterwards.
 */
@SpringBootTest
class StockConcurrencyTest {

    private static final int STARTING_STOCK = 10;

    @Autowired
    private SpareService spareService;
    @Autowired
    private RepairService repairService;
    @Autowired
    private MachineService machineService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private SpareRepository spareRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManager entityManager;

    private UUID departmentId;
    private UUID machineId;
    private UUID spareId;
    private UUID repairId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Department department = new Department();
            department.setDeptName("Concurrency-" + UUID.randomUUID());
            department.setPocName("POC");
            department.setPocNumber(6_600_000L);
            departmentRepository.saveAndFlush(department);
            departmentId = department.getId();

            Spare spare = new Spare();
            spare.setPartNumber("CONC-" + UUID.randomUUID());
            spare.setName("Concurrency spare");
            spare.setStock(STARTING_STOCK);
            spareRepository.saveAndFlush(spare);
            spareId = spare.getId();
        });

        machineId = TestSecurityContext.runAsManager(() -> machineService.createMachine(
                new CreateMachineRequest(
                        "CNC", "SN-" + UUID.randomUUID(), departmentId, null,
                        null, null, null, null, null, null,
                        null, null, null)).id());

        repairId = TestSecurityContext.runAsManager(() -> repairService.createRepair(
                new CreateRepairRequest(
                        machineId, RepairType.BREAKDOWN, RepairPriority.NORMAL,
                        "Concurrency", "conc-" + UUID.randomUUID(),
                        null, null, Instant.now()),
                TestSecurityContext.asManager()).id());
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("delete from repair_spares where spare_id = :spareId")
                    .setParameter("spareId", spareId).executeUpdate();
            entityManager.createNativeQuery("delete from repair_updates where repair_id = :repairId")
                    .setParameter("repairId", repairId).executeUpdate();
            entityManager.createNativeQuery("delete from repair_costs where repair_id = :repairId")
                    .setParameter("repairId", repairId).executeUpdate();
            entityManager.createNativeQuery("delete from repairs where id = :repairId")
                    .setParameter("repairId", repairId).executeUpdate();
            entityManager.createNativeQuery("delete from machines where id = :machineId")
                    .setParameter("machineId", machineId).executeUpdate();
            entityManager.createNativeQuery("delete from spares where id = :spareId")
                    .setParameter("spareId", spareId).executeUpdate();
            entityManager.createNativeQuery("delete from departments where id = :departmentId")
                    .setParameter("departmentId", departmentId).executeUpdate();
        });
    }

    /**
     * Two workers each try to issue more than half the stock on hand. With the row lock
     * only one can succeed, so the balance must never go negative and the granted total
     * must never exceed what existed.
     */
    @Test
    void concurrentIssuesCannotOversell() throws Exception {
        AtomicInteger granted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runConcurrently(
                () -> {
                    issue(7, granted, rejected);
                    return null;
                },
                () -> {
                    issue(7, granted, rejected);
                    return null;
                });

        int remaining = currentStock();
        assertThat(granted.get()).as("exactly one issue should win").isEqualTo(7);
        assertThat(rejected.get()).as("the other issue should be refused").isEqualTo(1);
        assertThat(remaining).as("stock must never go negative").isNotNegative();
        assertThat(granted.get() + remaining)
                .as("no quantity may be created or lost").isEqualTo(STARTING_STOCK);
    }

    /**
     * Concurrent receipts must all land. A read-modify-write without a row lock loses an
     * update and under-reports the balance.
     */
    @Test
    void concurrentReceiptsAreNotLost() throws Exception {
        int workers = 4;
        int each = 5;

        runConcurrently(
                () -> {
                    receive(each);
                    return null;
                },
                () -> {
                    receive(each);
                    return null;
                },
                () -> {
                    receive(each);
                    return null;
                },
                () -> {
                    receive(each);
                    return null;
                });

        assertThat(currentStock()).isEqualTo(STARTING_STOCK + workers * each);
    }

    private void issue(int quantity, AtomicInteger granted, AtomicInteger rejected) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    TestSecurityContext.runAs(TestSecurityContext.asManager(),
                            () -> spareService.issueStock(
                                    spareId, new StockIssueRequest(quantity, repairId))));
            granted.addAndGet(quantity);
        } catch (RuntimeException refused) {
            rejected.incrementAndGet();
        }
    }

    private void receive(int quantity) {
        transactionTemplate.executeWithoutResult(status ->
                TestSecurityContext.runAs(TestSecurityContext.asManager(),
                        () -> spareService.receiveStock(
                                spareId, new StockQuantityRequest(quantity))));
    }

    /**
     * Runs the supplied actions simultaneously, releasing them together so they contend
     * for the same row, and waits for every one to finish.
     */
    @SafeVarargs
    private final void runConcurrently(Callable<Void>... actions) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(actions.length);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> action : actions) {
                futures.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    action.call();
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private int currentStock() {
        Integer stock = transactionTemplate.execute(status -> {
            Object value = entityManager
                    .createNativeQuery("select stock from spares where id = :id")
                    .setParameter("id", spareId)
                    .getSingleResult();
            return value instanceof Number number ? number.intValue() : null;
        });
        return stock == null ? -1 : stock;
    }
}