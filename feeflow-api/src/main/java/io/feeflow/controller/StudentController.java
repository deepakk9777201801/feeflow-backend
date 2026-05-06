package io.feeflow.controller;

import common.dto.ApiResponse;
import common.dto.student.BulkImportRequest;
import common.dto.student.BulkImportResponse;
import common.dto.student.CreateStudentRequest;
import common.dto.student.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import students.student.service.StudentService;

import java.util.List;

@Tag(name = "Student", description = "Student management APIs")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Create a new student", description = "Enrolls a student in a batch; requires INSTITUTE_ADMIN role")
    @PostMapping
    @PreAuthorize("@instituteSecurityService.isAdmin(#request.instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created successfully", studentService.createStudent(request)));
    }

    @Operation(summary = "Get students by institute", description = "Returns students for an institute, optionally filtered by batchId; requires membership")
    @GetMapping
    @PreAuthorize("@instituteSecurityService.isMember(#instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @RequestParam Integer instituteId,
            @RequestParam(required = false) Integer batchId
    ) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudents(instituteId, batchId)));
    }

    @Operation(summary = "Get student by ID", description = "Returns a student by ID; requires institute membership")
    @GetMapping("/{id}")
    @PreAuthorize("@studentSecurityService.isMember(#id, authentication.name)")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentById(id)));
    }

    @Operation(summary = "Bulk import students (JSON)", description = "Imports multiple students via JSON payload; requires INSTITUTE_ADMIN role.")
    @PostMapping("/bulk-import")
    @PreAuthorize("@instituteSecurityService.isAdmin(#request.instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<BulkImportResponse>> bulkImport(
            @Valid @RequestBody BulkImportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk import completed", studentService.bulkImport(request)));
    }

    @Operation(summary = "Bulk import students (File)", description = "Imports students from a CSV or Excel (.xlsx) file. Expected columns: name, parent_name, primary_phone, whatsapp_phone, enrollment_date (yyyy-MM-dd). Requires INSTITUTE_ADMIN role.")
    @PostMapping(value = "/bulk-import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@instituteSecurityService.isAdmin(#instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<BulkImportResponse>> bulkImportFromFile(
            @RequestParam Integer instituteId,
            @RequestParam Integer batchId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File import completed", studentService.bulkImportFromFile(file, instituteId, batchId)));
    }
}
