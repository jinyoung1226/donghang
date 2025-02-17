package com.ebiz.wsb.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "ScheduleType")
public class ScheduleType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;
}
