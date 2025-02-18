package com.ebiz.wsb.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Uploader {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;

    /**
     * 파일을 S3에 업로드하는 메서드
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        // 파일 이름에서 공백을 제거한 새로운 파일 이름 생성
        String originalFileName = multipartFile.getOriginalFilename();

        // 파일 이름 중복 방지를 위해 UUID(고유한 식별자)를 생성해서 파일명 앞에 붙임
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "_" + originalFileName.replaceAll("\\s", "_");

        // S3에 저장될 경로 설정 -> dirName(폴더 이름)/고유한파일이름
        String fileName = dirName + "/" + uniqueFileName;
        log.info("fileName: " + fileName);
        File uploadFile = convert(multipartFile);

        // 반환된 파일을 S3에 업로드, 업로드된 파일의 URL을 가져옴
        String uploadImageUrl = putS3(uploadFile, fileName);

        // 로컬에 임시로 생성한 파일 삭제
        removeNewFile(uploadFile);
        return uploadImageUrl;
    }

    /**
     * MultipartFile(사용자 업로드 파일)을 File로 변환하는 메서드 (서버 로컬에 임시 파일로 저장)
     */
    private File convert(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "_" + originalFileName.replaceAll("\\s", "_");

        File convertFile = new File(uniqueFileName);
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            } catch (IOException e) {
                log.error("파일 변환 중 오류 발생: {}", e.getMessage());
                throw e;
            }
            return convertFile;
        }
        throw new IllegalArgumentException(String.format("파일 변환에 실패했습니다. %s", originalFileName));
    }

    /**
     * 변환된 File 객체를 S3에 업로드하는 메서드
     */
    private String putS3(File uploadFile, String fileName) {
        amazonS3.putObject(new PutObjectRequest(bucket, fileName, uploadFile));
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    /**
     * 임시로 만든 로컬 파일 삭제 (서버 저장공간 절약 목적)
     */
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("파일이 삭제되었습니다.");
        } else {
            log.info("파일이 삭제되지 못했습니다.");
        }
    }

    /**
     * 버킷 내 파일 지우는 메서드
     */
    public void deleteFile(String fileName) {
        try {
            String decodedFileName = URLDecoder.decode(fileName, "UTF-8");
            amazonS3.deleteObject(bucket, decodedFileName);
        } catch (UnsupportedEncodingException e) {
            log.error("파일명 디코딩 중 에러 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 외부에서 이미지 업로드할 때 사용하는 단순화된 메서드
     */
    public String uploadImage(MultipartFile imageFile, String dirName) {
        try {
            return upload(imageFile, dirName);
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }
}
