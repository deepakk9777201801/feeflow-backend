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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import students.student.service.FileParserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentServiceImpl Unit Tests")
class StudentServiceImplTest {

    @Mock private StudentRepository studentRepository;
    @Mock private InstituteRepository instituteRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private FileParserService fileParserService;

    @InjectMocks
    private StudentServiceImpl studentService;

    private static final Integer INSTITUTE_ID = 1;
    private static final Integer BATCH_ID     = 2;
    private static final Integer STUDENT_ID   = 10;

    private Institute testInstitute;
    private Batch     testBatch;
    private Student   testStudent;

    @BeforeEach
    void setUp() {
        testInstitute = Institute.builder()
                .id(INSTITUTE_ID).name("Test Institute")
                .city("Bangalore").state("Karnataka")
                .phone("9876543210").email("inst@test.com")
                .build();

        testBatch = Batch.builder()
                .id(BATCH_ID).institute(testInstitute)
                .name("Batch 2024").courseName("Java")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .status("ACTIVE").build();

        testStudent = Student.builder()
                .id(STUDENT_ID)
                .institute(testInstitute)
                .batch(testBatch)
                .name("John Doe")
                .parentName("Jane Doe")
                .primaryPhone("9000000001")
                .whatsappPhone("9000000001")
                .enrollmentDate(LocalDate.of(2024, 1, 15))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CreateStudentRequest buildCreateRequest() {
        return CreateStudentRequest.builder()
                .instituteId(INSTITUTE_ID)
                .batchId(BATCH_ID)
                .name("John Doe")
                .parentName("Jane Doe")
                .primaryPhone("9000000001")
                .whatsappPhone("9000000001")
                .enrollmentDate(LocalDate.of(2024, 1, 15))
                .status("ACTIVE")
                .build();
    }

    // ─── createStudent ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createStudent - success: saves student and returns mapped response")
    void createStudent_success() {
        when(studentRepository.existsByPrimaryPhoneAndInstituteId("9000000001", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        StudentResponse response = studentService.createStudent(buildCreateRequest());

        assertThat(response.getId()).isEqualTo(STUDENT_ID);
        assertThat(response.getInstituteId()).isEqualTo(INSTITUTE_ID);
        assertThat(response.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getParentName()).isEqualTo("Jane Doe");
        assertThat(response.getPrimaryPhone()).isEqualTo("9000000001");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getInstitute()).isEqualTo(testInstitute);
        assertThat(captor.getValue().getBatch()).isEqualTo(testBatch);
    }

    @Test
    @DisplayName("createStudent - defaults status to ACTIVE when null")
    void createStudent_nullStatus_defaultsToActive() {
        CreateStudentRequest request = buildCreateRequest();
        request.setStatus(null);

        when(studentRepository.existsByPrimaryPhoneAndInstituteId("9000000001", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        studentService.createStudent(request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("createStudent - throws BadRequestException when phone already registered in institute")
    void createStudent_duplicatePhone_throwsException() {
        when(studentRepository.existsByPrimaryPhoneAndInstituteId("9000000001", INSTITUTE_ID)).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("9000000001");

        verify(instituteRepository, never()).findById(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createStudent - throws ResourceNotFoundException when institute not found")
    void createStudent_instituteNotFound_throwsException() {
        when(studentRepository.existsByPrimaryPhoneAndInstituteId("9000000001", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createStudent(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(batchRepository, never()).findById(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createStudent - throws ResourceNotFoundException when batch not found")
    void createStudent_batchNotFound_throwsException() {
        when(studentRepository.existsByPrimaryPhoneAndInstituteId("9000000001", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createStudent(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(BATCH_ID));

        verify(studentRepository, never()).save(any());
    }

    // ─── getStudents ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudents - success: returns all students when batchId is null")
    void getStudents_noBatchFilter_returnsAllForInstitute() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(studentRepository.findByInstituteId(INSTITUTE_ID)).thenReturn(List.of(testStudent));

        List<StudentResponse> responses = studentService.getStudents(INSTITUTE_ID, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("John Doe");
        verify(studentRepository, never()).findByInstituteIdAndBatchId(anyInt(), anyInt());
    }

    @Test
    @DisplayName("getStudents - success: filters by batchId when provided")
    void getStudents_withBatchFilter_returnsFilteredStudents() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(studentRepository.findByInstituteIdAndBatchId(INSTITUTE_ID, BATCH_ID)).thenReturn(List.of(testStudent));

        List<StudentResponse> responses = studentService.getStudents(INSTITUTE_ID, BATCH_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBatchId()).isEqualTo(BATCH_ID);
        verify(studentRepository, never()).findByInstituteId(anyInt());
    }

    @Test
    @DisplayName("getStudents - returns empty list when no students found")
    void getStudents_noStudents_returnsEmpty() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(studentRepository.findByInstituteId(INSTITUTE_ID)).thenReturn(List.of());

        List<StudentResponse> responses = studentService.getStudents(INSTITUTE_ID, null);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("getStudents - throws ResourceNotFoundException when institute not found")
    void getStudents_instituteNotFound_throwsException() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudents(INSTITUTE_ID, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(studentRepository, never()).findByInstituteId(anyInt());
    }

    // ─── getStudentById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentById - success: returns all mapped fields")
    void getStudentById_success() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(testStudent));

        StudentResponse response = studentService.getStudentById(STUDENT_ID);

        assertThat(response.getId()).isEqualTo(STUDENT_ID);
        assertThat(response.getInstituteId()).isEqualTo(INSTITUTE_ID);
        assertThat(response.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getParentName()).isEqualTo("Jane Doe");
        assertThat(response.getPrimaryPhone()).isEqualTo("9000000001");
        assertThat(response.getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getStudentById - throws ResourceNotFoundException when not found")
    void getStudentById_notFound_throwsException() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(STUDENT_ID));
    }

    // ─── bulkImport ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("bulkImport - success: all rows created when no duplicates")
    void bulkImport_allSuccess() {
        BulkImportRequest request = buildBulkRequest(List.of(
                buildStudentData("Alice", "9111111111"),
                buildStudentData("Bob", "9222222222")
        ));

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.findPrimaryPhonesByInstituteId(INSTITUTE_ID)).thenReturn(Set.of());
        when(studentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkImportResponse response = studentService.bulkImport(request);

        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(0);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("bulkImport - partial: skips duplicates and reports errors")
    void bulkImport_partialDuplicates_reportsCorrectCounts() {
        BulkImportRequest request = buildBulkRequest(List.of(
                buildStudentData("Alice", "9111111111"),
                buildStudentData("Bob", "9222222222"),
                buildStudentData("Carol", "9333333333")
        ));

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.findPrimaryPhonesByInstituteId(INSTITUTE_ID)).thenReturn(Set.of("9222222222"));
        when(studentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkImportResponse response = studentService.bulkImport(request);

        assertThat(response.getTotalRows()).isEqualTo(3);
        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).contains("Bob").contains("9222222222");
    }

    @Test
    @DisplayName("bulkImport - all duplicates: creates zero students")
    void bulkImport_allDuplicates_createsNone() {
        BulkImportRequest request = buildBulkRequest(List.of(
                buildStudentData("Alice", "9111111111"),
                buildStudentData("Bob", "9222222222")
        ));

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.findPrimaryPhonesByInstituteId(INSTITUTE_ID)).thenReturn(Set.of("9111111111", "9222222222"));
        when(studentRepository.saveAll(anyList())).thenReturn(List.of());

        BulkImportResponse response = studentService.bulkImport(request);

        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getCreated()).isEqualTo(0);
        assertThat(response.getFailed()).isEqualTo(2);
        assertThat(response.getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("bulkImport - throws ResourceNotFoundException when institute not found")
    void bulkImport_instituteNotFound_throwsException() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.bulkImport(buildBulkRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(studentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("bulkImport - throws ResourceNotFoundException when batch not found")
    void bulkImport_batchNotFound_throwsException() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.bulkImport(buildBulkRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(BATCH_ID));

        verify(studentRepository, never()).saveAll(any());
    }

    // ─── bulkImportFromFile ───────────────────────────────────────────────────────

    @Test
    @DisplayName("bulkImportFromFile - success: parses file and delegates to bulkImport")
    void bulkImportFromFile_success() {
        MultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", "data".getBytes());
        List<BulkImportRequest.StudentData> parsed = List.of(
                buildStudentData("Alice", "9111111111"),
                buildStudentData("Bob", "9222222222")
        );

        when(fileParserService.parse(file)).thenReturn(parsed);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(studentRepository.findPrimaryPhonesByInstituteId(INSTITUTE_ID)).thenReturn(Set.of());
        when(studentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkImportResponse response = studentService.bulkImportFromFile(file, INSTITUTE_ID, BATCH_ID);

        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(0);
        verify(fileParserService).parse(file);
    }

    @Test
    @DisplayName("bulkImportFromFile - propagates BadRequestException when file parser fails")
    void bulkImportFromFile_parserFails_propagatesException() {
        MultipartFile file = new MockMultipartFile("file", "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "bad".getBytes());

        when(fileParserService.parse(file)).thenThrow(new BadRequestException("Failed to parse Excel file"));

        assertThatThrownBy(() -> studentService.bulkImportFromFile(file, INSTITUTE_ID, BATCH_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Failed to parse Excel file");

        verify(studentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("bulkImportFromFile - throws ResourceNotFoundException when institute not found")
    void bulkImportFromFile_instituteNotFound_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", "data".getBytes());

        when(fileParserService.parse(file)).thenReturn(List.of(buildStudentData("Alice", "9111111111")));
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.bulkImportFromFile(file, INSTITUTE_ID, BATCH_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(studentRepository, never()).saveAll(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private BulkImportRequest buildBulkRequest(List<BulkImportRequest.StudentData> students) {
        return BulkImportRequest.builder()
                .instituteId(INSTITUTE_ID)
                .batchId(BATCH_ID)
                .students(students)
                .build();
    }

    private BulkImportRequest.StudentData buildStudentData(String name, String phone) {
        return BulkImportRequest.StudentData.builder()
                .name(name)
                .parentName("Parent of " + name)
                .primaryPhone(phone)
                .whatsappPhone(phone)
                .enrollmentDate(LocalDate.of(2024, 1, 15))
                .build();
    }
}
