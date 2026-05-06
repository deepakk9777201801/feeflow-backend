package students.batch.service;

import common.dto.batch.BatchResponse;
import common.dto.batch.CreateBatchRequest;
import common.dto.batch.UpdateBatchRequest;

import java.util.List;

public interface BatchService {

    BatchResponse createBatch(CreateBatchRequest request);

    List<BatchResponse> getBatchesByInstituteId(Integer instituteId);

    BatchResponse updateBatch(Integer id, UpdateBatchRequest request);

    void deleteBatch(Integer id);
}
