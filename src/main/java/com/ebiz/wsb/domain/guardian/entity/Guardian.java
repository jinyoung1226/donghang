package com.ebiz.wsb.domain.guardian.entity;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.schedule.entity.Schedule;
import com.ebiz.wsb.domain.student.entity.Student;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "Guardian")
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "bio")
    private String bio;

    @Column(name = "experience")
    private String experience;

    @Column(name = "phone")
    private String phone;

    @Column(name = "image_path")
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToMany(mappedBy = "guardians")
    private List<Schedule> schedules;

}
