package students.student.service.impl;

import common.dto.student.BulkImportRequest.StudentData;
import common.exception.BadRequestException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileParserServiceImpl Unit Tests")
class FileParserServiceImplTest {

    private FileParserServiceImpl fileParserService;

    @BeforeEach
    void setUp() {
        fileParserService = new FileParserServiceImpl();
    }

    // ─── File type validation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("parse - throws BadRequestException for unsupported file type")
    void parse_unsupportedFileType_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf",
                "application/pdf", "pdf-content".getBytes());

        assertThatThrownBy(() -> fileParserService.parse(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("application/pdf")
                .hasMessageContaining("Only .csv and .xlsx files are supported");
    }

    @Test
    @DisplayName("parse - throws BadRequestException for unknown content-type with non-csv/xlsx filename")
    void parse_unknownContentTypeAndFilename_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt",
                "text/plain", "some text".getBytes());

        assertThatThrownBy(() -> fileParserService.parse(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only .csv and .xlsx files are supported");
    }

    // ─── CSV parsing ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("parse CSV - success: parses all fields correctly")
    void parseCsv_success() throws IOException {
        byte[] csvBytes = buildCsv(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"},
                {"Alice", "Alice Parent", "9111111111", "9111111111", "2024-01-15"},
                {"Bob",   "Bob Parent",   "9222222222", "9222222222", "2024-02-20"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csvBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).hasSize(2);

        StudentData alice = result.get(0);
        assertThat(alice.getName()).isEqualTo("Alice");
        assertThat(alice.getParentName()).isEqualTo("Alice Parent");
        assertThat(alice.getPrimaryPhone()).isEqualTo("9111111111");
        assertThat(alice.getWhatsappPhone()).isEqualTo("9111111111");
        assertThat(alice.getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 1, 15));

        StudentData bob = result.get(1);
        assertThat(bob.getName()).isEqualTo("Bob");
        assertThat(bob.getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 2, 20));
    }

    @Test
    @DisplayName("parse CSV - detects CSV by filename when content-type is generic")
    void parseCsv_detectedByFilename() throws IOException {
        byte[] csvBytes = buildCsv(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"},
                {"Carol", "Carol Parent", "9333333333", "", "2024-03-01"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.csv",
                "application/octet-stream", csvBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Carol");
    }

    @Test
    @DisplayName("parse CSV - throws BadRequestException on invalid date format")
    void parseCsv_invalidDate_throwsException() throws IOException {
        byte[] csvBytes = buildCsv(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"},
                {"Alice", "Alice Parent", "9111111111", "9111111111", "15-01-2024"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csvBytes);

        assertThatThrownBy(() -> fileParserService.parse(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("15-01-2024");
    }

    @Test
    @DisplayName("parse CSV - empty file returns empty list")
    void parseCsv_onlyHeader_returnsEmptyList() throws IOException {
        byte[] csvBytes = buildCsv(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csvBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).isEmpty();
    }

    // ─── Excel parsing ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("parse Excel - success: parses all fields correctly")
    void parseExcel_success() throws IOException {
        byte[] xlsxBytes = buildExcel(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"},
                {"Dave", "Dave Parent", "9444444444", "9444444444", "2024-04-10"},
                {"Eve",  "Eve Parent",  "9555555555", "9555555555", "2024-05-15"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Dave");
        assertThat(result.get(0).getEnrollmentDate()).isEqualTo(LocalDate.of(2024, 4, 10));
        assertThat(result.get(1).getName()).isEqualTo("Eve");
    }

    @Test
    @DisplayName("parse Excel - detects xlsx by filename when content-type is generic")
    void parseExcel_detectedByFilename() throws IOException {
        byte[] xlsxBytes = buildExcel(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"},
                {"Frank", "Frank Parent", "9666666666", "9666666666", "2024-06-01"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx",
                "application/octet-stream", xlsxBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Frank");
    }

    @Test
    @DisplayName("parse Excel - empty sheet (header only) returns empty list")
    void parseExcel_onlyHeader_returnsEmptyList() throws IOException {
        byte[] xlsxBytes = buildExcel(new String[][]{
                {"name", "parent_name", "primary_phone", "whatsapp_phone", "enrollment_date"}
        });

        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        List<StudentData> result = fileParserService.parse(file);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parse Excel - throws BadRequestException on corrupted file bytes")
    void parseExcel_corruptedFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not-valid-xlsx-content".getBytes());

        assertThatThrownBy(() -> fileParserService.parse(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Failed to parse Excel file");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private byte[] buildCsv(String[][] rows) throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            for (String[] row : rows) {
                printer.printRecord((Object[]) row);
            }
        }
        return sw.toString().getBytes();
    }

    private byte[] buildExcel(String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("Students");
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
