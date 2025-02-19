package com.ebiz.wsb.domain.student.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupAccessDeniedException;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.student.dto.StudentMapper;
import com.ebiz.wsb.domain.student.dto.StudentUpdateNotesRequestDTO;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotAccessException;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.student.repository.StudentRepository;
import com.ebiz.wsb.global.exception.InvalidUserTypeException;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    @Value("${cloud.aws.s3.Object.student}")
    private String StudentDirName;
    private final StudentRepository studentRepository;
    private final AuthorizationHelper authorizationHelper;
    private final StudentMapper studentMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final S3Uploader s3Uploader;

    /**
     * 지도사가 자신의 그룹에 배정된 학생을 조회하는 메서드
     * @param studentId 조회할 학생 ID
     * @return 조회된 학생 DTO
     */
    @Transactional
    public StudentDTO getStudentsByGuardian(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

        Guardian guardian = authorizationHelper.getLoggedInGuardian();
        Group guardianGroup = guardian.getGroup();

        if (guardianGroup == null || !guardianGroup.equals(student.getGroup())) {
            throw new GroupAccessDeniedException("해당 그룹의 학생 정보를 조회할 권한이 없습니다.");
        }

        return studentMapper.convertToStudentDTO(student, true);
    }

    /**
     * 학부모가 자신의 자녀를 조회하는 메서드
     * @return 조회된 학생 DTO
     */
    @Transactional
    public StudentDTO getStudentsByParent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));

        Parent parent = authorizationHelper.getLoggedInParent();
        if(!parent.getStudents().contains(student)) {
            throw new StudentNotAccessException("해당 학생 정보를 조회할 권한이 없습니다.");
        }
        return studentMapper.convertToStudentDTO(student, true);
    }

    /**
     * 지도사는 자신의 그룹에 배정된 학생들을 조회, 학부모는 자신의 자녀를 조회하는 메서드
     * @return 학생 리스트 DTO
     */
    public List<StudentDTO> getAllStudents() {
        Object currentUser = authorizationHelper.getCurrentUser();

        // 지도사인 경우
        if (currentUser instanceof Guardian guardian) {
            Group group = guardian.getGroup();
            if (group == null) {
                throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
            }
            List<Student> students = studentRepository.findAllByGroupId(group.getId());
            return students.stream()
                    .map(student -> studentMapper.convertToStudentDTO(student, true))
                    .toList();
        // 학부모인 경우
        } else if (currentUser instanceof Parent parent) {
            List<Student> students = studentRepository.findAllByParentId(parent.getId());
            return students.stream()
                    .map(student -> studentMapper.convertToStudentDTO(student, true))
                    .toList();
        }
        throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
    }


    /**
     * 학부모가 자신의 자녀 프로필을 생성하는 메서드
     * @param name 학생 이름
     * @param schoolName 학생 학교
     * @param grade 학생 학년
     * @param notes 학생 특이사항
     * @param parentPhone 학생의 학부모 전화번호
     * @param imageFile 학생 프로필 사진
     * @return 생성한 학생 정보 반환
     */
    @Transactional
    public StudentDTO createStudent(String name,String schoolName,String grade,String notes,String parentPhone, MultipartFile imageFile) {
        Parent parent = authorizationHelper.getLoggedInParent();

        String imagePath = s3Uploader.uploadImage(imageFile, StudentDirName);

        Student student = Student.builder()
                .name(name)
                .schoolName(schoolName)
                .grade(grade)
                .notes(notes)
                .imagePath(imagePath)
                .parent(parent)
                .ParentPhone(parentPhone)
                .build();

        studentRepository.save(student);

        return studentMapper.convertToStudentDTO(student, true);
    }


    /**
     * 학생의 특이사항을 수정하는 메서드
     * @param requestDTO 학생의 ID와 특이사항이 들어있는 DTO
     */
    @Transactional
    public void updateStudentNote(StudentUpdateNotesRequestDTO requestDTO) {
        Student existingStudent = studentRepository.findById(requestDTO.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));
        Parent parent = authorizationHelper.getLoggedInParent();

        // 본인의 자녀인지 확인
        if(!parent.getStudents().contains(existingStudent)) {
            throw new StudentNotAccessException("해당 학생 정보를 조회할 권한이 없습니다.");
        }

        Student updatedStudent = existingStudent.toBuilder()
                .notes(requestDTO.getNotes())
                .build();

        studentRepository.save(updatedStudent);
    }

    /**
     * 학생의 이미지를 수정하는 메서드
     * @param imageFile 학생의 업데이트할 이미지 파일
     * @param studentId 해당 학생의 ID
     */
    public void updateStudentImage(MultipartFile imageFile, Long studentId) {
        Student existingStudent = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));
        Parent parent = authorizationHelper.getLoggedInParent();

        // 본인의 자녀인지 확인
        if(!parent.getStudents().contains(existingStudent)) {
            throw new StudentNotAccessException("해당 학생 정보를 조회할 권한이 없습니다.");
        }

        String photoUrl = s3Uploader.uploadImage(imageFile, StudentDirName);

        Student updatedStudent = existingStudent.toBuilder()
                .imagePath(photoUrl)
                .build();

        studentRepository.save(updatedStudent);
    }

    /**
     * 학생을 삭제하는 메서드
     * @param studentId 삭제한 학생 ID
     */
    @Transactional
    public void deleteStudent(Long studentId) {
        Student existingStudent = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다."));
        Parent parent = authorizationHelper.getLoggedInParent();

        // 본인의 자녀인지 확인
        if(!parent.getStudents().contains(existingStudent)) {
            throw new StudentNotAccessException("해당 학생 정보를 조회할 권한이 없습니다.");
        }

        if (existingStudent.getImagePath() != null) {
            String imageUrl = existingStudent.getImagePath();
            String fileKey = imageUrl.substring(imageUrl.indexOf("b-cube-web")); // S3 파일 키 추출
            s3Uploader.deleteFile(fileKey);
        }
        studentRepository.delete(existingStudent);
    }
}
