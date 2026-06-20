package com.maintainsoft.entity;

import com.maintainsoft.enums.OperationCondition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "machines",
        indexes = {
                @Index(name = "idx_machine_department", columnList = "department_id"),
                @Index(name = "idx_machine_status", columnList = "status"),
                @Index(name = "idx_machine_serial", columnList = "serial_number")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"serial_number"})
)
public class Machine extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(name = "serial_number", nullable = false)
  private String serialNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OperationCondition status = OperationCondition.OPERATIONAL;

  @Column(nullable = false)
  private boolean deleted = false;

  @Version
  private Long version;
}
