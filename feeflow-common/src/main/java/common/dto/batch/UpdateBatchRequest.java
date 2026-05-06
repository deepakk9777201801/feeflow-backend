package common.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBatchRequest {

    private String name;
    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
