package com.maintainsoft.entity;

import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repairs",
        indexes = {
                @Index(name = "idx_machine_repair", columnList = "machine_id"),
                @Index(name = "idx_repair_status", columnList = "status"),
                @Index(name = "idx_repair_priority", columnList = "priority"),
                @Index(name = "idx_repair_created", columnList = "created_at")
        }
)
@Getter
@Setter
public class Repair extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus repairStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RepairType repairType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RepairPriority repairPriority = RepairPriority.NORMAL;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant startDate;

    @Column
    private Instant endDate;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<RepairUpdate> updateList = new ArrayList<>();

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RepairSpare> sparesList = new ArrayList<>();

    @Version
    private Long version;


}
