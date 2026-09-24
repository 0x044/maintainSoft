package com.maintainsoft.repository;

import com.maintainsoft.entity.RepairUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepairUpdateRepository extends JpaRepository<RepairUpdate, UUID> {
    List<RepairUpdate> findByRepair_IdOrderByCreatedAtAsc(UUID repairId);
}
