package com.maintainsoft.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) //TODO implement custom time aware uuidv7 generator
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy; // stores email of the principal

    @LastModifiedBy
    @Column(nullable = false)
    private String updatedBy;
}
