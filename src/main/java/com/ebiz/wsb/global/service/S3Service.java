package com.ebiz.wsb.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3 amazonS3;

    public List<String> uploadImageFiles(List<MultipartFile> images, String bucketName) {
        List<String> uploadedPaths = new ArrayList<>();
        images.stream().forEach(image -> {
            try {
                String s = uploadImageFile(image, bucketName);
                uploadedPaths.add(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return uploadedPaths;
    }

    public String uploadImageFile(MultipartFile image, String bucketName) throws IOException {
        String fileName = image.getOriginalFilename();
        String extension = image.getOriginalFilename().substring(fileName.lastIndexOf(".") + 1);
        String uploadFileName = UUID.randomUUID().toString();

        InputStream inputStream = image.getInputStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/" + extension);
        metadata.setContentLength(bytes.length);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try {
            // Canned ACL 제거
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucketName, uploadFileName, byteArrayInputStream, metadata);

            // 실제 업로드 동작하는 부분
            amazonS3.putObject(putObjectRequest);
            return amazonS3.getUrl(bucketName, uploadFileName).toString();
        } catch (AmazonS3Exception e) {
            System.err.println("S3 업로드 오류: " + e.getMessage());
            throw e;
        } finally {
            byteArrayInputStream.close();
            inputStream.close();
        }
    }

    public void deleteImage(String imageAddress, String bucketName) {
        if (imageAddress == null) {
            return;
        }
        String key = getKeyFromProfileImageAddress(imageAddress);
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AmazonS3Exception("삭제할 때 오류");
        }
    }

    public String uploadJsonToS3(String bucketName, String jsonContent, String uploadFileName) {
        byte[] jsonBytes = (jsonContent).getBytes(StandardCharsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json; charset=UTF-8");
        metadata.setContentLength(jsonBytes.length);

        // S3에 업로드할 객체 생성
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, uploadFileName,
                new ByteArrayInputStream(jsonBytes), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        // JSON 파일을 S3에 업로드
        amazonS3.putObject(putObjectRequest);

        // 업로드된 JSON 파일의 URL 반환
        return amazonS3.getUrl(bucketName, uploadFileName).toString();
    }

    private String getKeyFromProfileImageAddress(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String decodingKey = URLDecoder.decode(url.getPath(), "UTF-8");
            return decodingKey.substring(1);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String uploadScheduleFile(MultipartFile file, String bucketName) throws IOException{
        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String uploadFileName = UUID.randomUUID().toString();

        InputStream inputStream = file.getInputStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);

        String contentType;
        if("xlsx".equals(extension)){
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(bytes.length);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try{
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucketName, uploadFileName, byteArrayInputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.Private);
            amazonS3.putObject(putObjectRequest);
            return amazonS3.getUrl(bucketName, uploadFileName).toString();
        } catch (AmazonS3Exception e){
            System.err.println("S3 업로드 오류: " + e.getMessage());
            throw e;
        } finally {
            byteArrayInputStream.close();
            inputStream.close();
        }
    }
}