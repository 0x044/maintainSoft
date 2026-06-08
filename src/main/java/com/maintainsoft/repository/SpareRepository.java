package com.maintainsoft.repository;

import com.maintainsoft.entity.Spare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpareRepository extends JpaRepository<Spare, Long> {
    Optional<Spare> findByPartNumber(String partNumber);
}
