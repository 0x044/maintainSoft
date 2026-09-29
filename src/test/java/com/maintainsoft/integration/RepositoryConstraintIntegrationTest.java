package com.maintainsoft.integration;

import com.maintainsoft.entity.Department;
import com.maintainsoft.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database-level guarantees that the API layer relies on: a stale writer cannot
 * overwrite a newer version, and a duplicate master-data name is rejected by the
 * database. Both tests run inside the rollback-only integration transaction.
 */
@SpringBootTest
@Transactional
class RepositoryConstraintIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void staleVersionUpdateIsRejected() {
        Department department = departmentRepository.saveAndFlush(
                newDepartment("Constraint-Lock-" + UUID.randomUUID()));
        UUID id = department.getId();

        Department firstWriter = departmentRepository.findById(id).orElseThrow();
        entityManager.detach(firstWriter);
        Department staleWriter = departmentRepository.findById(id).orElseThrow();
        entityManager.detach(staleWriter);

        firstWriter.setPocName("First writer");
        Department saved = departmentRepository.saveAndFlush(firstWriter);
        assertThat(saved.getVersion()).isEqualTo(1L);

        staleWriter.setPocName("Stale writer");
        assertThatThrownBy(() -> departmentRepository.saveAndFlush(staleWriter))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void duplicateDepartmentNameIsRejectedByTheDatabase() {
        String deptName = "Duplicate-" + UUID.randomUUID();
        departmentRepository.saveAndFlush(newDepartment(deptName));

        Department duplicate = newDepartment(deptName);

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Department newDepartment(String deptName) {
        Department department = new Department();
        department.setDeptName(deptName);
        department.setPocName("Constraint POC");
        department.setPocNumber(9_100_000L);
        return department;
    }
}
