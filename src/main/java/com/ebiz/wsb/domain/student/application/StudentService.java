package com.ebiz.wsb.domain.student.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.student.dto.StudentCreateRequestDTO;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.student.dto.StudentMapper;
import com.ebiz.wsb.domain.student.dto.StudentUpdateNotesRequestDTO;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotAccessException;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.student.repository.StudentRepository;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.ImageService;
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

    @Value("${cloud.aws.s3.reviewImageBucketName}")
    private String reviewImageBucketName;
    private final StudentRepository studentRepository;
    private final ImageService imageService;
    private final AuthorizationHelper authorizationHelper;
    private final StudentMapper studentMapper;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public StudentDTO createStudent(StudentCreateRequestDTO requestDTO, MultipartFile imageFile) {
        Parent parent = authorizationHelper.getLoggedInParent();

        String imagePath = imageService.uploadImage(imageFile, reviewImageBucketName);

        Student student = Student.builder()
                .name(requestDTO.getName())
                .schoolName(requestDTO.getSchoolName())
                .grade(requestDTO.getGrade())
                .notes(requestDTO.getNotes())
                .imagePath(imagePath)
                .parent(parent)
                .ParentPhone(requestDTO.getParentPhone())
                .build();

        return studentMapper.toDTO(studentRepository.save(student), true);
    }


    public List<StudentDTO> getAllStudents() {
        Object currentUser = userDetailsService.getUserByContextHolder();

        if (currentUser instanceof Guardian guardian) {
            // 지도사일 경우
            Group group = guardian.getGroup();
            if (group == null) {
                throw new GroupNotFoundException("해당 지도사는 그룹에 속해 있지 않습니다.");
            }
            List<Student> students = studentRepository.findAllByGroupId(group.getId());
            return students.stream()
                    .map(student -> studentMapper.toDTO(student, true))
                    .toList();

        } else if (currentUser instanceof Parent parent) {
            // 부모일 경우
            List<Student> students = studentRepository.findAllByParentId(parent.getId());
            return students.stream()
                    .map(student -> studentMapper.toDTO(student, true))
                    .toList();
        }

        throw new StudentNotAccessException("학생 정보를 조회할 권한이 없습니다.");
    }


    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long studentId) {
        Student student = findStudentById(studentId);
        Object currentUser = userDetailsService.getUserByContextHolder();

        if (currentUser instanceof Guardian guardian) {

            Group group = student.getGroup();
            if (group == null || !group.getId().equals(guardian.getGroup().getId())) {
                throw new StudentNotAccessException("해당 그룹의 학생 정보를 조회할 권한이 없습니다.");
            }

        } else if (currentUser instanceof Parent parent) {

            authorizationHelper.validateParentAccess(student.getParent(), parent.getId());

        } else {
            throw new StudentNotAccessException("학생 정보를 조회할 권한이 없습니다.");
        }

        return studentMapper.toDTO(student, true);
    }



    @Transactional
    public void updateStudentNote(StudentUpdateNotesRequestDTO studentUpdateNotesRequestDTO) {

        Student existingStudent = findStudentById(studentUpdateNotesRequestDTO.getStudentId());
        Long parentId = existingStudent.getParent().getId();

        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if(userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;

            if(parent.getId().equals(parentId)) {
                Student updateStudent = existingStudent.toBuilder()
                        .notes(studentUpdateNotesRequestDTO.getNotes())
                        .build();

                log.info(updateStudent.toString());

                studentRepository.save(updateStudent);
            } else {
                throw new ParentAccessException("본인의 정보만 조회할 수 있습니다.");
            }
        } else {
            throw new ParentNotFoundException("해당 학부모를 찾을 수 없습니다.");
        }
    }

    public void updateStudentImageFile(MultipartFile imageFile, Long studentId) {
        Student existingStudent = findStudentById(studentId);
        Long parentId = existingStudent.getParent().getId();

        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if(userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;

            if(parent.getId().equals(parentId)) {
                String photoUrl = imageService.uploadImage(imageFile, reviewImageBucketName);
                Student updateStudent = existingStudent.toBuilder()
                        .imagePath(photoUrl)
                        .build();

                studentRepository.save(updateStudent);
            } else {
                throw new ParentAccessException("본인의 정보만 조회할 수 있습니다.");
            }
        } else {
            throw new ParentNotFoundException("해당 학부모를 찾을 수 없습니다.");
        }
    }


    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = findStudentById(studentId);
        Parent parent = authorizationHelper.getLoggedInParent();
        authorizationHelper.validateParentAccess(student.getParent(), parent.getId());

        if (student.getImagePath() != null) {
            imageService.deleteImage(student.getImagePath(), reviewImageBucketName);
        }

        studentRepository.delete(student);
    }

    private Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생을 찾을 수 없습니다."));
    }
}
