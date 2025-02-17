package com.ebiz.wsb.domain.group.entity;

import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
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
@Table(name = "`Group`")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "group_image")
    private String groupImage;

    @Column(name = "is_guide_active", nullable = false)
    private Boolean isGuideActive = false;

    @Column(name = "duty_guardian_id")
    private Long dutyGuardianId;

    @Column(name = "shuttle_status", nullable = false )
    private Boolean shuttleStatus = false;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Guardian> guardians = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Parent> parents = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<Waypoint> waypoints = new ArrayList<>();
}
