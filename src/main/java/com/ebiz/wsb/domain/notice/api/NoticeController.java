package com.ebiz.wsb.domain.notice.api;

import com.ebiz.wsb.domain.notice.application.NoticeService;
import com.ebiz.wsb.domain.notice.dto.NoticeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<Page<NoticeDTO>> getAllNotices(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        Page<NoticeDTO> notices = noticeService.getAllNotices(Pageable.ofSize(size).withPage(page));
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeDTO> getNoticeById(@PathVariable Long id){
        NoticeDTO noticeDTO = noticeService.getNoticeById(id);
        return ResponseEntity.ok(noticeDTO);
    }

    @PostMapping
    public ResponseEntity<NoticeDTO> createNotice(@Valid @RequestBody NoticeDTO newNoticeDTO) {
        NoticeDTO createdNotice = noticeService.createNotice(newNoticeDTO);
        return new ResponseEntity<>(createdNotice, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoticeDTO> updateNotice(@PathVariable Long id, @RequestBody NoticeDTO updatedNoticeDTO) {
        NoticeDTO updated = noticeService.updateNotice(id, updatedNoticeDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
