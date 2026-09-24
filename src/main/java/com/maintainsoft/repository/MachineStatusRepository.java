package com.maintainsoft.repository;

import com.maintainsoft.entity.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MachineStatusRepository extends JpaRepository<MachineStatus, UUID> {

    List<MachineStatus> findAllByOrderByBuiltInDescNameAsc();

    Optional<MachineStatus> findBySystemKey(String systemKey);
}
