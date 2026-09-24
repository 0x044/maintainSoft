package com.maintainsoft.repository;

import com.maintainsoft.entity.RepairCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepairCostRepository extends JpaRepository<RepairCost, UUID> {
    List<RepairCost> findByRepair_IdOrderByCreatedAtAsc(UUID repairId);
}
