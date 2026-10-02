package com.maintainsoft.integration;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.SpareRepository;
import com.maintainsoft.service.DepartmentService;
import com.maintainsoft.service.SpareService;
import com.maintainsoft.testsupport.TestSecurityContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ArchiveSemanticsIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SpareRepository spareRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SpareService spareService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void departmentArchiveHidesRecordAndUpdatesAuditMetadata() {
        Department department = new Department();
        department.setDeptName("Archive-" + UUID.randomUUID());
        department.setPocName("Archive POC");
        department.setPocNumber(8_800_000L);
        departmentRepository.saveAndFlush(department);
        UUID id = department.getId();

        TestSecurityContext.runAs(TestSecurityContext.asManager(),
                () -> departmentService.deleteDepartment(id));

        assertThat(department.isDeleted()).isTrue();
        assertThat(department.getUpdatedAt()).isNotNull();
        // The auditor resolves the caller when the transaction flushes, which happens
        // after the temporary context is restored, so it still falls back to "system".
        assertThat(department.getUpdatedBy()).isEqualTo("system");
        entityManager.flush();
        entityManager.clear();
        assertThat(departmentRepository.findById(id)).isEmpty();
    }

    @Test
    void spareArchiveHidesRecordFromActiveQueries() {
        Spare spare = new Spare();
        spare.setPartNumber("ARCHIVE-" + UUID.randomUUID());
        spare.setName("Archive spare");
        spare.setStock(1);
        spareRepository.saveAndFlush(spare);
        UUID id = spare.getId();

        TestSecurityContext.runAs(TestSecurityContext.asSupervisor(),
                () -> spareService.archiveSpare(id));

        assertThat(spare.isDeleted()).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(spareRepository.findById(id)).isEmpty();
    }
}
