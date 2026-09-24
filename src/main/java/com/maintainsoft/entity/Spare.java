package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "spares",
        uniqueConstraints = @UniqueConstraint(columnNames = "part_number"),
        indexes = {
                @Index(name = "idx_spare_part_number", columnList = "part_number"),
                @Index(name = "idx_spare_quantity", columnList = "stock")
        }
)
@SQLRestriction("deleted = false")
@Getter
@Setter
public class Spare extends BaseEntity {

    @Column(unique = true)
    private String partNumber;

    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 32)
    private String unit;

    @Column(name = "compatible_machine", length = 255)
    private String compatibleMachine;

    @Column(precision = 19, scale = 4)
    private BigDecimal cost;

    @LastModifiedDate
    private Instant lastPurchaseDate;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean deleted = false;

    @Version
    private Long version;
}
