package common.dto.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkImportRequest {

    @NotNull(message = "Institute ID is required")
    private Integer instituteId;

    @NotNull(message = "Batch ID is required")
    private Integer batchId;

    @NotEmpty(message = "Student list must not be empty")
    @Valid
    private List<StudentData> students;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentData {

        @NotBlank(message = "Student name is required")
        private String name;

        @NotBlank(message = "Parent name is required")
        private String parentName;

        @NotBlank(message = "Primary phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Primary phone must be 10-15 digits")
        private String primaryPhone;

        @Pattern(regexp = "^[0-9]{10,15}$", message = "WhatsApp phone must be 10-15 digits")
        private String whatsappPhone;

        @NotNull(message = "Enrollment date is required")
        private LocalDate enrollmentDate;
    }
}
