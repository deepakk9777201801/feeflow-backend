package io.feeflow.controller;

import common.dto.ApiResponse;
import common.dto.institute.CreateInstituteRequest;
import common.dto.institute.InstituteResponse;
import common.dto.institute.UpdateInstituteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import students.institute.service.InstituteService;

import java.util.List;

@Tag(name = "Institute", description = "Institute management APIs")
@RestController
@RequestMapping("/api/v1/institutes")
@RequiredArgsConstructor
public class InstituteController {

    private final InstituteService instituteService;

    @Operation(summary = "Create a new institute", description = "Creates an institute and assigns the caller as INSTITUTE_ADMIN")
    @PostMapping
    public ResponseEntity<ApiResponse<InstituteResponse>> createInstitute(
            @Valid @RequestBody CreateInstituteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Institute created successfully", instituteService.createInstitute(request)));
    }

    @Operation(summary = "Get my institutes", description = "Returns all institutes the current user belongs to")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InstituteResponse>>> getMyInstitutes() {
        return ResponseEntity.ok(ApiResponse.success(instituteService.getMyInstitutes()));
    }

    @Operation(summary = "Get institute by ID", description = "Returns an institute by ID; requires membership")
    @GetMapping("/{id}")
    @PreAuthorize("@instituteSecurityService.isMember(#id, authentication.name)")
    public ResponseEntity<ApiResponse<InstituteResponse>> getInstituteById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(instituteService.getInstituteById(id)));
    }

    @Operation(summary = "Update institute", description = "Updates an institute; requires INSTITUTE_ADMIN role")
    @PutMapping("/{id}")
    @PreAuthorize("@instituteSecurityService.isAdmin(#id, authentication.name)")
    public ResponseEntity<ApiResponse<InstituteResponse>> updateInstitute(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateInstituteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Institute updated successfully", instituteService.updateInstitute(id, request)));
    }
}
