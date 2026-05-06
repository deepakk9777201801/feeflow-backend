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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import students.batch.service.BatchService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private static final String DELETED_STATUS = "DELETED";

    private final BatchRepository batchRepository;
    private final InstituteRepository instituteRepository;

    @Override
    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        log.info("Creating batch '{}' for institute: {}", request.getName(), request.getInstituteId());

        if (batchRepository.existsByNameAndInstituteId(request.getName(), request.getInstituteId())) {
            throw new BadRequestException("A batch named '" + request.getName() + "' already exists in this institute");
        }

        Institute institute = instituteRepository.findById(request.getInstituteId())
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId()));

        Batch batch = Batch.builder()
                .institute(institute)
                .name(request.getName())
                .courseName(request.getCourseName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Optional.ofNullable(request.getStatus()).orElse("ACTIVE"))
                .build();

        Batch saved = batchRepository.save(batch);
        log.info("Batch '{}' created with id: {}", saved.getName(), saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> getBatchesByInstituteId(Integer instituteId) {
        log.debug("Fetching batches for institute: {}", instituteId);

        instituteRepository.findById(instituteId)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + instituteId));

        return batchRepository.findActiveByInstituteId(instituteId, DELETED_STATUS).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(Integer id, UpdateBatchRequest request) {
        log.info("Updating batch with id: {}", id);

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));

        Optional.ofNullable(request.getName()).ifPresent(batch::setName);
        Optional.ofNullable(request.getCourseName()).ifPresent(batch::setCourseName);
        Optional.ofNullable(request.getStartDate()).ifPresent(batch::setStartDate);
        Optional.ofNullable(request.getEndDate()).ifPresent(batch::setEndDate);
        Optional.ofNullable(request.getStatus()).ifPresent(batch::setStatus);

        Batch updated = batchRepository.save(batch);
        log.info("Batch '{}' updated successfully", updated.getId());

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(Integer id) {
        log.info("Soft-deleting batch with id: {}", id);

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));

        batch.setStatus(DELETED_STATUS);
        batchRepository.save(batch);
        log.info("Batch '{}' marked as DELETED", id);
    }

    private BatchResponse toResponse(Batch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .instituteId(batch.getInstitute().getId())
                .name(batch.getName())
                .courseName(batch.getCourseName())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .build();
    }
}
