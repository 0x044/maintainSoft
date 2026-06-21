package com.maintainsoft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "technicians")
@Getter
@Setter
public class Technician extends BaseEntity{
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;
}
