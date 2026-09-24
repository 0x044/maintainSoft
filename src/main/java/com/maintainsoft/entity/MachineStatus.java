package com.maintainsoft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "machine_statuses")
@SQLRestriction("deleted = false")
@Getter
@Setter
public class MachineStatus extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "system_key", unique = true, length = 64)
    private String systemKey;

    @Column(nullable = false)
    private boolean builtIn;

    @Column(nullable = false)
    private boolean deleted = false;

    @jakarta.persistence.Version
    private Long version;
}
