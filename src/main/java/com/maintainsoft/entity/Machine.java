package com.maintainsoft.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
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
                @Index(name = "idx_machine_status_id", columnList = "status_id"),
                @Index(name = "idx_machine_serial", columnList = "serial_number")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"serial_number"})
)
@SQLRestriction("deleted = false")
public class Machine extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(name = "serial_number", nullable = false)
  private String serialNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  private MachineStatus status;

  @Column(nullable = false)
  private boolean deleted = false;

  @Version
  private Long version;
}
