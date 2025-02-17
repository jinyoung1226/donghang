package com.ebiz.wsb.domain.group.api;

import com.ebiz.wsb.domain.group.application.GroupService;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.student.dto.StudentUpdateNotesRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;

    // 출근하기 (인솔자가 "출근하기" 누르면 누른 인솔자 ID로 그룹 출근 상태 업데이트)
    @PostMapping("/start-guide")
    public ResponseEntity<GroupDTO> startGuide() {
        GroupDTO groupDTO = groupService.startGuide();
        return ResponseEntity.ok(groupDTO);
    }

    // 퇴근하기 (인솔자가 "퇴근하기" 누르면 그룹 출근 상태 종료)
    @PostMapping("/stop-guide")
    public ResponseEntity<GroupDTO> stopGuide() {
        GroupDTO groupDTO = groupService.stopGuide();
        return ResponseEntity.ok(groupDTO);
    }

    @GetMapping("/guide-status")
    public ResponseEntity<GroupDTO> getGuideStatus() {
        GroupDTO guideStatus = groupService.getGuideStatus();
        return ResponseEntity.ok(guideStatus);
    }

    @PatchMapping("/update/image")
    public ResponseEntity<Void> updateStudentImage(@RequestPart MultipartFile imageFile, @RequestPart Long groupId ) {
        groupService.updateStudentImage(imageFile, groupId);
        return ResponseEntity.ok().build();
    }
}
