package com.ebiz.wsb.domain.notice.application;

import com.ebiz.wsb.domain.notice.dto.NoticeDTO;
import com.ebiz.wsb.domain.notice.entity.Notice;
import com.ebiz.wsb.domain.notice.exception.NoticeNotFoundException;
import com.ebiz.wsb.domain.notice.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public Page<NoticeDTO> getAllNotices(Pageable pageable) {
        Page<Notice> notices = noticeRepository.findAll(pageable);

        // 만약 빈 배열이라면 빈 Page 객체 반환
        if (notices.isEmpty()) {
            return Page.empty(pageable);
        }

        return notices.map(this::convertToDTO);
    }


    public NoticeDTO getNoticeById(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
        return convertToDTO(notice);
    }

    public NoticeDTO createNotice(NoticeDTO noticeDTO) {
        Notice notice = convertToEntity(noticeDTO);
        Notice savedNotice = noticeRepository.save(notice);
        return convertToDTO(savedNotice);
    }

    public NoticeDTO updateNotice(Long noticeId, NoticeDTO updatedNoticeDTO) {
        return noticeRepository.findById(noticeId)
                .map(existingNotice -> {
                    Notice updatedNotice = Notice.builder()
                            .noticeId(existingNotice.getNoticeId())
                            .title(updatedNoticeDTO.getTitle())
                            .content(updatedNoticeDTO.getContent())
                            .createdAt(existingNotice.getCreatedAt())
                            .build();
                    Notice savedNotice = noticeRepository.save(updatedNotice);
                    return convertToDTO(savedNotice);
                })
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
    }

    public void deleteNotice(Long noticeId) {
        noticeRepository.deleteById(noticeId);
    }

    private NoticeDTO convertToDTO(Notice notice) {
        return NoticeDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt())
                .build();
    }

    private Notice convertToEntity(NoticeDTO noticeDTO) {
        return Notice.builder()
                .noticeId(noticeDTO.getNoticeId())
                .title(noticeDTO.getTitle())
                .content(noticeDTO.getContent())
                .createdAt(noticeDTO.getCreatedAt())
                .build();
    }
}