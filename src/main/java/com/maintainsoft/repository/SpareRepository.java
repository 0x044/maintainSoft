package com.maintainsoft.repository;

import com.maintainsoft.entity.Spare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpareRepository extends JpaRepository<Spare, UUID> {
    Optional<Spare> findByPartNumber(String partNumber);
}
