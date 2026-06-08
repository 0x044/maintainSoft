package com.maintainsoft.repository;

import com.maintainsoft.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByDepartment(String department);
}
