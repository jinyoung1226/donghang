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

    @GetMapping("/guardian")
    public ResponseEntity<StudentDTO> getStudentsByGuardian(@PathVariable Long studentId) {
        StudentDTO studentDTO = studentService.getStudentsByGuardian(studentId);
        return ResponseEntity.ok(studentDTO);
    }

    @GetMapping("/parent")
    public ResponseEntity<StudentDTO> getStudentsByParent(@PathVariable Long studentId) {
        StudentDTO studentDTO = studentService.getStudentsByParent(studentId);
        return ResponseEntity.ok(studentDTO);
    }

    @GetMapping("/all")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(
            @RequestParam("name") String name,
            @RequestParam("schoolName") String schoolName,
            @RequestParam("grade") String grade,
            @RequestParam("notes") String notes,
            @RequestParam("parentPhone") String parentPhone,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        StudentDTO createdStudent = studentService.createStudent(name, schoolName, grade, notes, parentPhone, imageFile);
        return ResponseEntity.ok(createdStudent);
    }

    @PatchMapping("/update/notes")
    public ResponseEntity<Void> updateStudentNote(@RequestBody StudentUpdateNotesRequestDTO studentUpdateNotesRequestDTO) {
        studentService.updateStudentNote(studentUpdateNotesRequestDTO);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update/imageFile")
    public ResponseEntity<Void> updateStudentImage(@RequestPart(value = "imageFile", required = false) MultipartFile imageFile, @RequestParam("studentId") Long studentId) {
        studentService.updateStudentImage(imageFile, studentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }
}
