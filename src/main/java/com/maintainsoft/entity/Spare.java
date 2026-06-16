package com.maintainsoft.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "Spares")
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
}
