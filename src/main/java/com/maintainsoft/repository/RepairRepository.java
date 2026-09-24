package com.maintainsoft.repository;

import com.maintainsoft.entity.Repair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepairRepository extends JpaRepository<Repair, UUID> {
}
