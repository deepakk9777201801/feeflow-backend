package students.student.service;

import common.dto.student.BulkImportRequest;
import common.dto.student.BulkImportResponse;
import common.dto.student.CreateStudentRequest;
import common.dto.student.StudentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    StudentResponse createStudent(CreateStudentRequest request);

    List<StudentResponse> getStudents(Integer instituteId, Integer batchId);

    StudentResponse getStudentById(Integer id);

    BulkImportResponse bulkImport(BulkImportRequest request);

    BulkImportResponse bulkImportFromFile(MultipartFile file, Integer instituteId, Integer batchId);
}
