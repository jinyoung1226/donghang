package com.ebiz.wsb.domain.student.entity;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "StudentProfile")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;


    @Column(name = "name")
    private String name;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "grade")
    private String grade;

    @Column(name = "notes")
    private String notes;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "parent_phone")
    private String ParentPhone;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "waypoint_id")
    private Waypoint waypoint;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Parent parent;
}
