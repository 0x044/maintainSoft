package com.maintainsoft.entity;

import com.maintainsoft.enums.OperationCondition;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "machines")
public class Machine extends BaseEntity {

  private String name;

  @ManyToOne
  private Department department;

  @Enumerated(EnumType.STRING)
  private OperationCondition operationCondition;
}
