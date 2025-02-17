package com.ebiz.wsb.domain.message.entity;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@Table(name = "Message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    @Column(name = "content")
    private String content;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(nullable = false, name = "is_read")
    private boolean isRead = false;
}
