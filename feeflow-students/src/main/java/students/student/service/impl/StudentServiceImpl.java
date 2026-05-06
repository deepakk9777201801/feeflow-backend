package students.student.service.impl;

import auth.entity.Batch;
import auth.entity.Institute;
import auth.entity.Student;
import auth.repository.BatchRepository;
import auth.repository.InstituteRepository;
import auth.repository.StudentRepository;
import common.dto.student.BulkImportRequest;
import common.dto.student.BulkImportResponse;
import common.dto.student.CreateStudentRequest;
import common.dto.student.StudentResponse;
import common.exception.BadRequestException;
import common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import students.student.service.FileParserService;
import students.student.service.StudentService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final InstituteRepository instituteRepository;
    private final BatchRepository batchRepository;
    private final FileParserService fileParserService;

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("Creating student '{}' for institute: {}", request.getName(), request.getInstituteId());

        if (studentRepository.existsByPrimaryPhoneAndInstituteId(request.getPrimaryPhone(), request.getInstituteId())) {
            throw new BadRequestException("A student with phone '" + request.getPrimaryPhone() + "' already exists in this institute");
        }

        Institute institute = instituteRepository.findById(request.getInstituteId())
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId()));

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + request.getBatchId()));

        Student student = Student.builder()
                .institute(institute)
                .batch(batch)
                .name(request.getName())
                .parentName(request.getParentName())
                .primaryPhone(request.getPrimaryPhone())
                .whatsappPhone(request.getWhatsappPhone())
                .enrollmentDate(request.getEnrollmentDate())
                .status(Optional.ofNullable(request.getStatus()).orElse("ACTIVE"))
                .build();

        Student saved = studentRepository.save(student);
        log.info("Student '{}' created with id: {}", saved.getName(), saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudents(Integer instituteId, Integer batchId) {
        log.debug("Fetching students for institute: {}, batchId: {}", instituteId, batchId);

        instituteRepository.findById(instituteId)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + instituteId));

        return Optional.ofNullable(batchId)
                .map(id -> studentRepository.findByInstituteIdAndBatchId(instituteId, id))
                .orElseGet(() -> studentRepository.findByInstituteId(instituteId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Integer id) {
        log.debug("Fetching student with id: {}", id);
        return studentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    @Override
    @Transactional
    public BulkImportResponse bulkImport(BulkImportRequest request) {
        log.info("Bulk importing {} students for institute: {}", request.getStudents().size(), request.getInstituteId());

        Institute institute = instituteRepository.findById(request.getInstituteId())
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId()));

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + request.getBatchId()));

        Set<String> existingPhones = studentRepository.findPrimaryPhonesByInstituteId(request.getInstituteId());

        Map<Boolean, List<BulkImportRequest.StudentData>> partitioned = request.getStudents().stream()
                .collect(Collectors.partitioningBy(
                        data -> !existingPhones.contains(data.getPrimaryPhone())));

        List<String> errors = partitioned.get(false).stream()
                .map(data -> "Skipped '" + data.getName() + "': phone " + data.getPrimaryPhone() + " already registered")
                .collect(Collectors.toList());

        List<Student> toSave = partitioned.get(true).stream()
                .map(data -> Student.builder()
                        .institute(institute)
                        .batch(batch)
                        .name(data.getName())
                        .parentName(data.getParentName())
                        .primaryPhone(data.getPrimaryPhone())
                        .whatsappPhone(data.getWhatsappPhone())
                        .enrollmentDate(data.getEnrollmentDate())
                        .status("ACTIVE")
                        .build())
                .collect(Collectors.toList());

        List<Student> saved = studentRepository.saveAll(toSave);

        int total = request.getStudents().size();
        int created = saved.size();
        int failed = total - created;

        log.info("Bulk import complete — total: {}, created: {}, failed: {}", total, created, failed);

        return BulkImportResponse.builder()
                .totalRows(total)
                .created(created)
                .failed(failed)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public BulkImportResponse bulkImportFromFile(MultipartFile file, Integer instituteId, Integer batchId) {
        log.info("File-based bulk import for institute: {}, batch: {}", instituteId, batchId);

        BulkImportRequest request = BulkImportRequest.builder()
                .instituteId(instituteId)
                .batchId(batchId)
                .students(fileParserService.parse(file))
                .build();

        return bulkImport(request);
    }

    private StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .instituteId(student.getInstitute().getId())
                .batchId(student.getBatch().getId())
                .name(student.getName())
                .parentName(student.getParentName())
                .primaryPhone(student.getPrimaryPhone())
                .whatsappPhone(student.getWhatsappPhone())
                .enrollmentDate(student.getEnrollmentDate())
                .status(student.getStatus())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
