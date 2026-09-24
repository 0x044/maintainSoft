package com.maintainsoft.repository;

import com.maintainsoft.entity.Spare;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpareRepository extends JpaRepository<Spare, UUID> {
    Optional<Spare> findByPartNumber(String partNumber);

    java.util.List<Spare> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select spare from Spare spare where spare.id = :id")
    Optional<Spare> findByIdForUpdate(@Param("id") UUID id);
}
