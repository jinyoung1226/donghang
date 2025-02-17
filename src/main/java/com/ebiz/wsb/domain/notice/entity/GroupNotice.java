package com.ebiz.wsb.domain.notice.entity;


import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "GroupNotice")
public class GroupNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_notice_id")
    private Long groupNoticeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @Column(name = "content")
    private String content;

    @Column(name = "likes")
    private Integer likes;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "group_id")
    private Group group;

    @OneToMany(mappedBy = "groupNotice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GroupNoticePhoto> photos = new ArrayList<>();

    public void incrementLikes(){
        this.likes++;
    }

    public void decrementLikes(){
        if(this.likes > 0){
            this.likes--;
        }
    }

}
