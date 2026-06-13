package com.maintainsoft.entity;

import com.maintainsoft.enums.OperationCondition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "Machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "machine_name")
    private String name;

    @ManyToOne
    private Department department;

    @CreationTimestamp
    private LocalDateTime startedOn;

    @Enumerated(EnumType.STRING)
    private OperationCondition operationCondition;

}
