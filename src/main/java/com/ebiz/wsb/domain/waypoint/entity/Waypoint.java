package com.ebiz.wsb.domain.waypoint.entity;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "Waypoint")
public class Waypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waypoint_id")
    private Long id;

    @Column(name = "waypoint_name")
    private String waypointName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "waypoint_order")
    private Integer waypointOrder;

    @Column(name = "attendance_complete", nullable = false)
    private Boolean attendanceComplete = false;

    @Column(name = "current_count")
    private int currentCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "waypoint")
    private List<Student> students = new ArrayList<>();

}
