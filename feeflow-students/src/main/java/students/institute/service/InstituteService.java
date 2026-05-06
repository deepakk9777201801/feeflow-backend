package students.institute.service;

import common.dto.institute.CreateInstituteRequest;
import common.dto.institute.InstituteResponse;
import common.dto.institute.UpdateInstituteRequest;

import java.util.List;

public interface InstituteService {

    InstituteResponse createInstitute(CreateInstituteRequest request);

    List<InstituteResponse> getMyInstitutes();

    InstituteResponse getInstituteById(Integer id);

    InstituteResponse updateInstitute(Integer id, UpdateInstituteRequest request);
}
