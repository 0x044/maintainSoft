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
@Table(name = "machines")
public class Machine extends BaseEntity{

    private String name;

    @ManyToOne
    private Department department;

    @Enumerated(EnumType.STRING)
    private OperationCondition operationCondition;
}
