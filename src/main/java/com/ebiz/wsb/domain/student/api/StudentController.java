package com.ebiz.wsb.domain.student.api;

import com.ebiz.wsb.domain.student.dto.*;
import com.ebiz.wsb.domain.student.application.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(
            @RequestParam("name") String name,
            @RequestParam("schoolName") String schoolName,
            @RequestParam("grade") String grade,
            @RequestParam("notes") String notes,
            @RequestParam("parentPhone") String parentPhone,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        StudentCreateRequestDTO studentCreateRequestDTO = StudentCreateRequestDTO.builder()
                .name(name)
                .schoolName(schoolName)
                .grade(grade)
                .notes(notes)
                .parentPhone(parentPhone)
                .build();

        StudentDTO createdStudent = studentService.createStudent(studentCreateRequestDTO, imageFile);
        return ResponseEntity.ok(createdStudent);
    }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long studentId) {
        StudentDTO studentDTO = studentService.getStudentById(studentId);
        return ResponseEntity.ok(studentDTO);
    }


    @PatchMapping("/update/notes")
    public ResponseEntity<Void> updateStudentNote(@RequestBody StudentUpdateNotesRequestDTO studentUpdateNotesRequestDTO) {
        studentService.updateStudentNote(studentUpdateNotesRequestDTO);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update/imageFile")
    public ResponseEntity<Void> updateStudentImageFile(@RequestPart(value = "imageFile", required = false) MultipartFile imageFile, @RequestParam("studentId") Long studentId) {
        studentService.updateStudentImageFile(imageFile, studentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }
}
