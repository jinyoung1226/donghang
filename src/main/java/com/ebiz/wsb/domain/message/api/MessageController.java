package com.ebiz.wsb.domain.message.api;

import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.message.application.MessageService;
import com.ebiz.wsb.domain.message.dto.MessageDTO;
import com.ebiz.wsb.domain.message.dto.MessageSendRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody MessageSendRequestDTO messageSendRequestDTO) {
        messageService.sendMessage(messageSendRequestDTO);
        return ResponseEntity.ok("메시지가 성공적으로 전달되었습니다.");
    }

    @GetMapping("/received/{studentId}")
    public ResponseEntity<List<MessageDTO>> getMessagesForGuardian(@PathVariable Long studentId) {
        try {
            List<MessageDTO> messagesForGuardian = messageService.getMessagesForGuardian(studentId);

            // 비어있어도 빈 리스트 반환
            return ResponseEntity.ok(messagesForGuardian);
        } catch (GuardianNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/received/one/{studentId}")
    public ResponseEntity<?> getMessagesForGuardianOne(@PathVariable Long studentId) {
        List<MessageDTO> messagesForGuardianOne = messageService.getMessagesForGuardianOne(studentId);
        return ResponseEntity.ok(messagesForGuardianOne);
    }

}
