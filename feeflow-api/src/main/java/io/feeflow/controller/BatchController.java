package io.feeflow.controller;

import common.dto.ApiResponse;
import common.dto.batch.BatchResponse;
import common.dto.batch.CreateBatchRequest;
import common.dto.batch.UpdateBatchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import students.batch.service.BatchService;

import java.util.List;

@Tag(name = "Batch", description = "Batch management APIs")
@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "Create a new batch", description = "Creates a batch within an institute; requires INSTITUTE_ADMIN role")
    @PostMapping
    @PreAuthorize("@instituteSecurityService.isAdmin(#request.instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(
            @Valid @RequestBody CreateBatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch created successfully", batchService.createBatch(request)));
    }

    @Operation(summary = "Get batches by institute", description = "Returns all active batches for an institute; requires membership")
    @GetMapping
    @PreAuthorize("@instituteSecurityService.isMember(#instituteId, authentication.name)")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getBatchesByInstituteId(
            @RequestParam Integer instituteId
    ) {
        return ResponseEntity.ok(ApiResponse.success(batchService.getBatchesByInstituteId(instituteId)));
    }

    @Operation(summary = "Update a batch", description = "Partial update of a batch; requires INSTITUTE_ADMIN role")
    @PutMapping("/{id}")
    @PreAuthorize("@batchSecurityService.isAdmin(#id, authentication.name)")
    public ResponseEntity<ApiResponse<BatchResponse>> updateBatch(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBatchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Batch updated successfully", batchService.updateBatch(id, request)));
    }

    @Operation(summary = "Delete a batch", description = "Soft-deletes a batch (status=DELETED); requires INSTITUTE_ADMIN role")
    @DeleteMapping("/{id}")
    @PreAuthorize("@batchSecurityService.isAdmin(#id, authentication.name)")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable Integer id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.success("Batch deleted successfully", null));
    }
}
