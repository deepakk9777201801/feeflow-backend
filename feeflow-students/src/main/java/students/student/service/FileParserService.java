package students.student.service;

import common.dto.student.BulkImportRequest.StudentData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileParserService {

    List<StudentData> parse(MultipartFile file);
}
