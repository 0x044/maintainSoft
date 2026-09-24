package com.maintainsoft.repository;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<Machine, UUID> {
    List<Machine> findByDepartment(Department department);

    List<Machine> findByDepartment_IdOrderByNameAsc(UUID departmentId);

    List<Machine> findByStatus_IdOrderByNameAsc(UUID statusId);

    List<Machine> findByDepartment_IdAndStatus_IdOrderByNameAsc(UUID departmentId, UUID statusId);

    List<Machine> findAllByOrderByNameAsc();
}
