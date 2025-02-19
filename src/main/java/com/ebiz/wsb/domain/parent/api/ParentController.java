package com.ebiz.wsb.domain.parent.api;

import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.parent.application.ParentService;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.ebiz.wsb.global.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/parents")
@Slf4j
public class ParentController {

    private final ParentService parentService;

    @GetMapping
    public ResponseEntity<ParentDTO> getParent() {
        ParentDTO parentInfo = parentService.getParent();
        return ResponseEntity.ok(parentInfo);
    }

    @GetMapping("/group")
    public ResponseEntity<GroupDTO> getParentGroup() {
        GroupDTO group = parentService.getParentGroup();
        return new ResponseEntity<>(group, HttpStatus.OK);
    }

    @GetMapping("/shuttle-status")
    public ResponseEntity<GroupDTO> getShuttleStatus() {
        GroupDTO shuttleStatus = parentService.getShuttleStatus();
        return ResponseEntity.ok(shuttleStatus);
    }

    @PutMapping
    public ResponseEntity<ParentDTO> updateParent(
            @RequestBody ParentDTO parentDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        ParentDTO updatedParentDTO = parentService.updateParent(parentDTO, imageFile);
        return ResponseEntity.ok(updatedParentDTO);
    }

    @DeleteMapping()
    public ResponseEntity<BaseResponse> deleteParent() {
        parentService.deleteParent();
        return ResponseEntity.ok(BaseResponse.builder().message("학부모 정보가 삭제되었습니다.").build());
    }
}
