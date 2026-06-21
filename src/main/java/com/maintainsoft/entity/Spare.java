package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
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
@SQLDelete(sql = "UPDATE spares SET deleted = true where id = ? and version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class Spare extends BaseEntity {

    @Column(unique = true)
    private String partNumber;

    private String name;

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
