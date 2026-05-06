package common.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkImportResponse {

    private int totalRows;
    private int created;
    private int failed;
    private List<String> errors;
}
