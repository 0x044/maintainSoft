package com.maintainsoft.repository;

import com.maintainsoft.entity.Repair;
import com.maintainsoft.enums.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairRepository extends JpaRepository<Repair, UUID> {
    Optional<Repair> findByIdempotencyKey(String idempotencyKey);

    List<Repair> findAllByOrderByCreatedAtDesc();

    List<Repair> findByRepairStatusOrderByCreatedAtDesc(RepairStatus repairStatus);

    List<Repair> findByMachine_IdOrderByCreatedAtDesc(UUID machineId);
}
