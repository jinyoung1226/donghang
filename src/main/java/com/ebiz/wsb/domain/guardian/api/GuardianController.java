package com.ebiz.wsb.domain.guardian.api;

import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.guardian.application.GuardianService;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.student.dto.StudentUpdateNotesRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/guardian")
@RequiredArgsConstructor
public class GuardianController {
    private final GuardianService guardianService;

    @GetMapping
    public ResponseEntity<GuardianDTO> getMyGuardianInfo() {
        GuardianDTO guardianDTO = guardianService.getMyGuardianInfo();
        return new ResponseEntity<>(guardianDTO, HttpStatus.OK);
    }

    @PatchMapping("/update/imageFile")
    public ResponseEntity<Void> updateGuardianImageFile(@RequestPart MultipartFile imageFile) {
        guardianService.updateGuardianImageFile(imageFile);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<String> deleteMyGuardianInfo() {
        guardianService.deleteMyGuardianInfo();
        log.info("Deleted logged-in Guardian information");
        return ResponseEntity.ok("정보가 성공적으로 삭제 되었습니다.");
    }

    @GetMapping("/group")
    public ResponseEntity<GroupDTO> getGuardianGroup() {
        GroupDTO group = guardianService.getGuardianGroup();
        return new ResponseEntity<>(group, HttpStatus.OK);
    }
}
