package com.maintainsoft.repository;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<Machine, UUID> {
    List<Machine> findByDepartment(Department department);
}
