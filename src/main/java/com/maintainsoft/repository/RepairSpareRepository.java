package com.maintainsoft.repository;

import com.maintainsoft.entity.RepairSpare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairSpareRepository
        extends JpaRepository<RepairSpare, RepairSpare.RepairSpareId> {
}
