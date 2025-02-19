package com.ebiz.wsb.domain.guardian.api;

import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.guardian.application.GuardianService;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/guardian")
@RequiredArgsConstructor
public class GuardianController {
    private final GuardianService guardianService;

    @GetMapping
    public ResponseEntity<GuardianDTO> getGuardian() {
        GuardianDTO guardianDTO = guardianService.getGuardian();
        return new ResponseEntity<>(guardianDTO, HttpStatus.OK);
    }

    @GetMapping("/group")
    public ResponseEntity<GroupDTO> getGuardianGroup() {
        GroupDTO group = guardianService.getGuardianGroup();
        return new ResponseEntity<>(group, HttpStatus.OK);
    }

    @PatchMapping("/update/imageFile")
    public ResponseEntity<Void> updateGuardian(@RequestPart MultipartFile imageFile) {
        guardianService.updateGuardian(imageFile);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<String> deleteGuardian() {
        guardianService.deleteGuardian();
        return ResponseEntity.ok("지도사 정보가 삭제되었습니다.");
    }
}
