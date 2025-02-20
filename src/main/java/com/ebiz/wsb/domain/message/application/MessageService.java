package com.ebiz.wsb.domain.message.application;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.message.dto.MessageDTO;
import com.ebiz.wsb.domain.message.dto.MessageSendRequestDTO;
import com.ebiz.wsb.domain.message.entity.Message;
import com.ebiz.wsb.domain.message.repository.MessageRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.student.repository.StudentRepository;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final PushNotificationService pushNotificationService;
    private final StudentRepository studentRepository;
    private final AuthorizationHelper authorizationHelper;

    /**
     * 학부모가 지도사에게 메시지를 보내는 메서드
     * @param messageSendRequestDTO 메시지 글 담은 DTO
     */
    @Transactional
    public void sendMessage(MessageSendRequestDTO messageSendRequestDTO) {
        // 학생 조회 및 검증
        Student student = studentRepository.findById(messageSendRequestDTO.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));
        Parent studentParent = student.getParent();

        // 현재 로그인한 학부모 가져오기
        Parent loggedInParent = authorizationHelper.getLoggedInParent();

        // 현재 학부모와 학생의 부모가 일치하는지 확인
        if (!loggedInParent.equals(studentParent)) {
            throw new ParentNotFoundException("학부모 정보를 찾을 수 없습니다.");
        }

        // 그룹 및 인솔자 조회
        Group group = loggedInParent.getGroup();
        if (group == null) {
            throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
        }

        List<Guardian> guardians = group.getGuardians();
        if (guardians.isEmpty()) {
            throw new GuardianNotFoundException("지도사 정보를 찾을 수 없습니다.");
        }

        // 메시지 생성 및 저장
        guardians.forEach(guardian -> {
            Message message = Message.builder()
                    .group(group)
                    .parent(loggedInParent)
                    .student(student)
                    .guardian(guardian)
                    .content(messageSendRequestDTO.getContent())
                    .transferredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();
            messageRepository.save(message);
        });

        // 메시지 보낼 때, 인솔자들에게 메시지 푸시알림 보내기
        Map<String, String> pushData = pushNotificationService.createPushData(PushType.MESSAGE);

        // title 내용에 학생 이름 삽입
        String titleWithStudentNames = String.format(
                pushData.get("title"),
                loggedInParent.getStudents().stream()
                        .map(Student::getName)
                        .collect(Collectors.joining(", "))
        );

        // body 내용에 메시지 내용 삽입
        String bodyWithContent = String.format(pushData.get("body"), messageSendRequestDTO.getContent());
        pushData.put("title", titleWithStudentNames);
        pushData.put("body", bodyWithContent);

        // 알림센터 title에 학생 이름 삽입
        String alarmTitleWithStudentName = String.format(pushData.get("guardian_alarm_center_title"), student.getName());
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
    }


    /**
     * 지도사가 특정 학부모(학생)으로부터 온 메시지 조회하는 메서드
     * @param studentId 학생 Id
     * @return 메시지 List DTO
     */
    @Transactional
    public List<MessageDTO> getMessagesForGuardian(Long studentId) {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        // 학생 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

        // 학생이 속한 그룹과 지도사의 그룹 일치 여부 확인
        Group studentGroup = student.getParent().getGroup();
        Group guardianGroup = loggedInGuardian.getGroup();

        if (studentGroup == null || !studentGroup.equals(guardianGroup)) {
            throw new GroupNotFoundException("해당 학생이 속한 그룹에 대한 접근 권한이 없습니다.");
        }

        // 학생과 지도사와 관련된 메시지 조회
        List<Message> messages = messageRepository.findByStudent_StudentIdAndGuardian_Id(studentId, loggedInGuardian.getId());

        // 메시지가 없으면 빈 리스트 반환
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 최신순 정렬
        messages.sort(Comparator.comparing(Message::getTransferredAt).reversed());

        // DTO 변환 및 읽지 않은 메시지 추출
        List<MessageDTO> messageDTOs = new ArrayList<>();
        List<Message> unreadMessages = new ArrayList<>();

        for (Message message : messages) {
            if (!message.isRead()) {
                unreadMessages.add(message);
            }
            messageDTOs.add(MessageDTO.builder()
                    .messageId(message.getMessageId())
                    .parent(toParentDTO(message.getParent()))
                    .content(message.getContent())
                    .transferredAt(message.getTransferredAt())
                    .isRead(message.isRead())
                    .build());
        }

        // 비동기 읽음 처리
        markMessagesAsReadAsync(unreadMessages);
        return messageDTOs;
    }


    /**
     * 지도사가 가장 최신의 학부모(학생)으로부터 온 메시지 한 개 확인하는 메서드
     * @param studentId
     * @return
     */
    @Transactional
    public List<MessageDTO> getMessagesForGuardianOne(Long studentId) {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        // 학생 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

        // 학생이 속한 그룹과 지도사의 그룹 일치 여부 확인
        Group studentGroup = student.getParent().getGroup();
        Group guardianGroup = loggedInGuardian.getGroup();

        if (studentGroup == null || !studentGroup.equals(guardianGroup)) {
            throw new GroupNotFoundException("해당 학생이 속한 그룹에 대한 접근 권한이 없습니다.");
        }

        // 특정 학생과 연관된 메시지만 조회
        List<Message> messages = messageRepository.findByStudent_StudentIdAndGuardian_Id(studentId, loggedInGuardian.getId());

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
