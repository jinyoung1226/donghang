package com.ebiz.wsb.global.service;

import com.ebiz.wsb.domain.guardian.exception.FileUploadException;
import com.ebiz.wsb.domain.student.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Service s3Service;

    public String uploadImage(MultipartFile imageFile, String bucketName) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        try {
            return s3Service.uploadImageFile(imageFile, bucketName);
        } catch (IOException e) {
            throw new ImageUploadException("이미지 업로드 실패", e);
        }
    }

    public void deleteImage(String imagePath, String bucketName) {
        try {
            s3Service.deleteImage(imagePath, bucketName);
        } catch (Exception e) {
            throw new FileUploadException("이미지 삭제 실패", e);
        }
    }
}
