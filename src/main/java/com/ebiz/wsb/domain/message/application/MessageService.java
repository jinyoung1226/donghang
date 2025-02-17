package com.ebiz.wsb.domain.message.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.message.dto.MessageDTO;
import com.ebiz.wsb.domain.message.dto.MessageSendRequestDTO;
import com.ebiz.wsb.domain.message.entity.Message;
import com.ebiz.wsb.domain.message.exception.MessageAccessException;
import com.ebiz.wsb.domain.message.repository.MessageRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.student.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final PushNotificationService pushNotificationService;
    private final StudentRepository studentRepository;

    @Transactional
    public void sendMessage(MessageSendRequestDTO messageSendRequestDTO) {
        // body에서 받은 studentId로 학생 조회
        Student existingStudent = studentRepository.findById(messageSendRequestDTO.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

        // 조회한 학생의 부모 조회
        Parent existingStudentParent = existingStudent.getParent();

        // 현재 사용자 정보(인증 객체)로 학부모 여부 확인
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;

            // 조회한 학부모와 인증객체 부모가 같은지 확인
            if (parent.getId().equals(existingStudentParent.getId())) {

                Group group = parent.getGroup();
                if (group == null) {
                    throw new GroupNotFoundException("부모가 속한 그룹이 없습니다.");
                }

                // 해당 그룹의 모든 인솔자에게 메시지 생성 및 저장
                List<Guardian> guardians = group.getGuardians();
                if (guardians.isEmpty()) {
                    throw new GuardianNotFoundException("그룹에 속한 인솔자가 없습니다.");
                }

                for (Guardian guardian : guardians) {
                    // 메시지 생성
                    Message message = Message.builder()
                            .group(group)
                            .parent(parent)
                            .student(existingStudent)
                            .guardian(guardian) // 각 메시지를 특정 인솔자와 연계
                            .content(messageSendRequestDTO.getContent())
                            .transferredAt(LocalDateTime.now())
                            .isRead(false) // 초기 읽음 상태는 false
                            .build();

                    messageRepository.save(message);
                }
              
                // 메시지 보낼 때, 인솔자들에게 메시지 푸시알림 보내기
                Map<String, String> pushData = pushNotificationService.createPushData(PushType.MESSAGE);

                // title 내용에 학생 이름 삽입
                String titleWithStudentNames = String.format(
                        pushData.get("title"),
                        parent.getStudents().stream()
                                .map(Student::getName)
                                .collect(Collectors.joining(", "))
                );

                // body 내용에 메시지 내용 삽입
                String bodyWithContent = String.format(pushData.get("body"), messageSendRequestDTO.getContent());
                pushData.put("title", titleWithStudentNames);
                pushData.put("body", bodyWithContent);

                // 알림센터 title에 학생 이름 삽입
                String alarmTitleWithStudentName = String.format(pushData.get("guardian_alarm_center_title"), existingStudent.getName());
                pushData.put("guardian_alarm_center_title", alarmTitleWithStudentName);

                // 알림센터 body에 메시지 내용 삽입
                String alarmBodyWithContent = String.format(pushData.get("guardian_alarm_center_body"), messageSendRequestDTO.getContent());
                pushData.put("guardian_alarm_center_body", alarmBodyWithContent);

                pushNotificationService.sendPushNotificationToGuardians(
                        group.getId(),
                        pushData.get("title"),
                        pushData.get("body"),
                        pushData.get("guardian_alarm_center_title"),
                        pushData.get("guardian_alarm_center_body"),
                        PushType.MESSAGE
                );
            } else {
                throw new ParentAccessException("조회한 학부모와 일치하지 않습니다.");
            }
        } else {
            throw new ParentNotFoundException("현재 사용자는 학부모가 아닙니다.");
        }
    }


    @Transactional
    public List<MessageDTO> getMessagesForGuardian(Long studentId) {
        // 현재 사용자 정보(인증 객체)로 지도사 여부 확인
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;

            // 학생 정보 조회
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

            // 학생의 그룹 확인
            Group group = student.getParent().getGroup();
            if (group == null) {
                throw new GroupNotFoundException("학생이 속한 그룹이 없습니다.");
            }

            // 현재 지도사가 그룹의 인솔자인지 확인
            if (group.getGuardians().stream().noneMatch(g -> g.getId().equals(guardian.getId()))) {
                throw new GuardianNotFoundException("해당 그룹의 인솔자가 아닙니다.");
            }

            // 특정 학생과 연관된 메시지만 조회
            List<Message> messages = messageRepository.findByStudent_StudentIdAndGuardian_Id(studentId, guardian.getId());

            if (messages.isEmpty()) {
                return Collections.emptyList();
            }

            // 최신 메시지가 먼저 오도록 정렬
            messages.sort((m1, m2) -> m2.getTransferredAt().compareTo(m1.getTransferredAt()));

            // 반환할 메시지 리스트 및 읽지 않은 메시지 리스트
            List<MessageDTO> messageDTOs = new ArrayList<>();
            List<Message> unreadMessages = new ArrayList<>();

            for (Message message : messages) {
                // 읽지 않은 메시지를 리스트에 추가
                if (!message.isRead()) {
                    unreadMessages.add(message);
                }

                // MessageDTO 생성 및 리스트에 추가
                messageDTOs.add(MessageDTO.builder()
                        .messageId(message.getMessageId())
                        .parent(toParentDTO(message.getParent()))
                        .content(message.getContent())
                        .transferredAt(message.getTransferredAt())
                        .isRead(message.isRead()) // 읽음 상태 포함
                        .build());
            }

            // 비동기적으로 읽음 상태를 업데이트
            markMessagesAsReadAsync(unreadMessages);

            return messageDTOs;
        } else {
            throw new MessageAccessException("지도사님이 아니면 메시지를 확인할 수 없습니다.");
        }
    }


    @Transactional
    public List<MessageDTO> getMessagesForGuardianOne(Long studentId) {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;

            // 학생 정보 조회
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

            // 학생의 그룹 확인
            Group group = student.getParent().getGroup();
            if (group == null) {
                throw new GroupNotFoundException("학생이 속한 그룹이 없습니다.");
            }

            // 현재 지도사가 그룹의 인솔자인지 확인
            if (group.getGuardians().stream().noneMatch(g -> g.getId().equals(guardian.getId()))) {
                throw new GuardianNotFoundException("해당 그룹의 인솔자가 아닙니다.");
            }

            // 특정 학생과 연관된 메시지만 조회
            List<Message> messages = messageRepository.findByStudent_StudentIdAndGuardian_Id(studentId, guardian.getId());

            // 메시지가 없는 경우 빈 리스트 반환
            if (messages.isEmpty()) {
                return Collections.emptyList();
            }

            // 최신이 먼저 오게 정렬
            messages.sort((r1,r2) -> r2.getTransferredAt().compareTo(r1.getTransferredAt()));

            // 최신 메시지 가져오기
            Message recentMessage = messages.get(0);

            // 메시지 DTO 생성
            MessageDTO messageDTO = MessageDTO.builder()
                    .messageId(recentMessage.getMessageId())
                    .parent(toParentDTO(recentMessage.getParent()))
                    .content(recentMessage.getContent())
                    .transferredAt(recentMessage.getTransferredAt())
                    .isRead(recentMessage.isRead())
                    .build();

            return Collections.singletonList(messageDTO); // 단일 메시지를 리스트로 반환
        }

        throw new MessageAccessException("지도사님이 아니면 메시지를 확인할 수 없습니다.");
    }



    @Async
    public void markMessagesAsReadAsync(List<Message> messages) {
        messages.forEach(message -> {
            message.setRead(true);   // 메시지의 읽음 상태를 true로 설정
            messageRepository.save(message);
        });
    }


    private ParentDTO toParentDTO(Parent parent) {
        return ParentDTO.builder()
                .id(parent.getId())
                .name(parent.getName())
                .imagePath(parent.getImagePath())
                .build();
    }
}
