package students.batch.service.impl;

import auth.entity.Batch;
import auth.entity.Institute;
import auth.repository.BatchRepository;
import auth.repository.InstituteRepository;
import common.dto.batch.BatchResponse;
import common.dto.batch.CreateBatchRequest;
import common.dto.batch.UpdateBatchRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchServiceImpl Unit Tests")
class BatchServiceImplTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private InstituteRepository instituteRepository;

    @InjectMocks
    private BatchServiceImpl batchService;

    private static final Integer INSTITUTE_ID = 1;
    private static final Integer BATCH_ID = 10;

    private Institute testInstitute;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        testInstitute = Institute.builder()
                .id(INSTITUTE_ID)
                .name("Test Institute")
                .city("Bangalore")
                .state("Karnataka")
                .phone("9876543210")
                .email("institute@test.com")
                .build();

        testBatch = Batch.builder()
                .id(BATCH_ID)
                .institute(testInstitute)
                .name("Batch 2024")
                .courseName("Java Programming")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CreateBatchRequest buildCreateRequest() {
        return CreateBatchRequest.builder()
                .instituteId(INSTITUTE_ID)
                .name("Batch 2024")
                .courseName("Java Programming")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .status("ACTIVE")
                .build();
    }

    // ─── createBatch ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBatch - success: creates batch and returns response")
    void createBatch_success() {
        when(batchRepository.existsByNameAndInstituteId("Batch 2024", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);

        BatchResponse response = batchService.createBatch(buildCreateRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(BATCH_ID);
        assertThat(response.getInstituteId()).isEqualTo(INSTITUTE_ID);
        assertThat(response.getName()).isEqualTo("Batch 2024");
        assertThat(response.getCourseName()).isEqualTo("Java Programming");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2024, 6, 30));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<Batch> captor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Batch 2024");
        assertThat(captor.getValue().getInstitute()).isEqualTo(testInstitute);
    }

    @Test
    @DisplayName("createBatch - defaults status to ACTIVE when null")
    void createBatch_nullStatus_defaultsToActive() {
        CreateBatchRequest request = CreateBatchRequest.builder()
                .instituteId(INSTITUTE_ID)
                .name("Batch 2024")
                .courseName("Java Programming")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .status(null)
                .build();

        when(batchRepository.existsByNameAndInstituteId("Batch 2024", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);

        batchService.createBatch(request);

        ArgumentCaptor<Batch> captor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("createBatch - throws BadRequestException when batch name already exists in institute")
    void createBatch_duplicateName_throwsException() {
        when(batchRepository.existsByNameAndInstituteId("Batch 2024", INSTITUTE_ID)).thenReturn(true);

        assertThatThrownBy(() -> batchService.createBatch(buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Batch 2024");

        verify(instituteRepository, never()).findById(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBatch - throws ResourceNotFoundException when institute not found")
    void createBatch_instituteNotFound_throwsException() {
        when(batchRepository.existsByNameAndInstituteId("Batch 2024", INSTITUTE_ID)).thenReturn(false);
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.createBatch(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(batchRepository, never()).save(any());
    }

    // ─── getBatchesByInstituteId ─────────────────────────────────────────────────

    @Test
    @DisplayName("getBatchesByInstituteId - success: returns only non-deleted batches (DB-filtered)")
    void getBatchesByInstituteId_success() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findActiveByInstituteId(INSTITUTE_ID, "DELETED")).thenReturn(List.of(testBatch));

        List<BatchResponse> responses = batchService.getBatchesByInstituteId(INSTITUTE_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Batch 2024");
        assertThat(responses.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getBatchesByInstituteId - returns empty list when no active batches")
    void getBatchesByInstituteId_noActiveBatches_returnsEmpty() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(batchRepository.findActiveByInstituteId(INSTITUTE_ID, "DELETED")).thenReturn(List.of());

        List<BatchResponse> responses = batchService.getBatchesByInstituteId(INSTITUTE_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("getBatchesByInstituteId - throws ResourceNotFoundException when institute not found")
    void getBatchesByInstituteId_instituteNotFound_throwsException() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.getBatchesByInstituteId(INSTITUTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(batchRepository, never()).findActiveByInstituteId(any(), any());
    }

    // ─── updateBatch ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBatch - success: updates all provided fields")
    void updateBatch_success_allFields() {
        UpdateBatchRequest request = UpdateBatchRequest.builder()
                .name("Updated Batch").courseName("Updated Course")
                .startDate(LocalDate.of(2024, 3, 1)).endDate(LocalDate.of(2024, 9, 30))
                .status("COMPLETED")
                .build();

        Batch updatedBatch = Batch.builder()
                .id(BATCH_ID).institute(testInstitute).name("Updated Batch")
                .courseName("Updated Course").startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 9, 30)).status("COMPLETED")
                .createdAt(testBatch.getCreatedAt()).build();

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(updatedBatch);

        BatchResponse response = batchService.updateBatch(BATCH_ID, request);

        assertThat(response.getName()).isEqualTo("Updated Batch");
        assertThat(response.getCourseName()).isEqualTo("Updated Course");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(batchRepository).save(testBatch);
    }

    @Test
    @DisplayName("updateBatch - partial update: only name provided")
    void updateBatch_partialUpdate_onlyNameChanged() {
        UpdateBatchRequest request = UpdateBatchRequest.builder().name("Only Name Updated").build();

        Batch afterSave = Batch.builder()
                .id(BATCH_ID).institute(testInstitute).name("Only Name Updated")
                .courseName("Java Programming").startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30)).status("ACTIVE")
                .createdAt(testBatch.getCreatedAt()).build();

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(afterSave);

        BatchResponse response = batchService.updateBatch(BATCH_ID, request);

        assertThat(response.getName()).isEqualTo("Only Name Updated");
        assertThat(response.getCourseName()).isEqualTo("Java Programming");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("updateBatch - no-op when all request fields are null")
    void updateBatch_allNullFields_noChanges() {
        UpdateBatchRequest request = new UpdateBatchRequest();

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);

        BatchResponse response = batchService.updateBatch(BATCH_ID, request);

        assertThat(response.getName()).isEqualTo("Batch 2024");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(batchRepository).save(testBatch);
    }

    @Test
    @DisplayName("updateBatch - throws ResourceNotFoundException when batch not found")
    void updateBatch_notFound_throwsException() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.updateBatch(BATCH_ID, new UpdateBatchRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(BATCH_ID));

        verify(batchRepository, never()).save(any());
    }

    // ─── deleteBatch ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBatch - success: sets status to DELETED")
    void deleteBatch_success() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);

        batchService.deleteBatch(BATCH_ID);

        ArgumentCaptor<Batch> captor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("deleteBatch - throws ResourceNotFoundException when batch not found")
    void deleteBatch_notFound_throwsException() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.deleteBatch(BATCH_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(BATCH_ID));

        verify(batchRepository, never()).save(any());
    }
}
