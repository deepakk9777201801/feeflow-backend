package students.student.service.impl;

import common.dto.student.BulkImportRequest.StudentData;
import common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import students.student.service.FileParserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class FileParserServiceImpl implements FileParserService {

    private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_CSV  = "text/csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<StudentData> parse(MultipartFile file) {
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        String filename    = Optional.ofNullable(file.getOriginalFilename()).orElse("");

        log.info("Parsing file '{}' with content-type '{}'", filename, contentType);
        validateFileType(contentType, filename);

        return isExcel(contentType, filename) ? parseExcel(file) : parseCsv(file);
    }

    private void validateFileType(String contentType, String filename) {
        boolean valid = isExcel(contentType, filename)
                || CONTENT_TYPE_CSV.equalsIgnoreCase(contentType)
                || filename.toLowerCase().endsWith(".csv");
        Optional.of(valid)
                .filter(v -> v)
                .orElseThrow(() -> new BadRequestException(
                        "Unsupported file type '" + contentType + "'. Only .csv and .xlsx files are supported"));
    }

    private boolean isExcel(String contentType, String filename) {
        return CONTENT_TYPE_XLSX.equalsIgnoreCase(contentType)
                || filename.toLowerCase().endsWith(".xlsx");
    }

    private List<StudentData> parseCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            return StreamSupport.stream(parser.spliterator(), false)
                    .map(this::recordToStudentData)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Failed to parse CSV file", e);
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    private List<StudentData> parseExcel(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new BadRequestException("Excel file is empty or missing header row");
            }

            int lastRow = sheet.getLastRowNum();

            return Optional.of(lastRow)
                    .filter(last -> last >= 1)
                    .map(last -> IntStream.rangeClosed(1, last)
                            .mapToObj(sheet::getRow)
                            .filter(Objects::nonNull)
                            .map(this::rowToStudentData)
                            .collect(Collectors.toList()))
                    .orElse(List.of());

        } catch (IOException | RuntimeException e) {
            log.error("Failed to parse Excel file", e);
            throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
        }
    }

    private StudentData recordToStudentData(CSVRecord record) {
        return StudentData.builder()
                .name(record.get("name"))
                .parentName(record.get("parent_name"))
                .primaryPhone(record.get("primary_phone"))
                .whatsappPhone(getOptionalCsvField(record, "whatsapp_phone"))
                .enrollmentDate(parseDate(record.get("enrollment_date")))
                .build();
    }

    private StudentData rowToStudentData(Row row) {
        return StudentData.builder()
                .name(getCellValue(row, 0))
                .parentName(getCellValue(row, 1))
                .primaryPhone(getCellValue(row, 2))
                .whatsappPhone(getCellValue(row, 3))
                .enrollmentDate(parseDateCell(row.getCell(4)))
                .build();
    }

    private String getCellValue(Row row, int index) {
        return Optional.ofNullable(row.getCell(index))
                .map(this::cellToString)
                .orElse(null);
    }

    private String cellToString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private LocalDate parseDateCell(Cell cell) {
        return Optional.ofNullable(cell)
                .map(c -> switch (c.getCellType()) {
                    case NUMERIC -> DateUtil.isCellDateFormatted(c)
                            ? c.getLocalDateTimeCellValue().toLocalDate()
                            : parseDate(String.valueOf((long) c.getNumericCellValue()));
                    case STRING  -> parseDate(c.getStringCellValue().trim());
                    default      -> null;
                })
                .orElse(null);
    }

    private LocalDate parseDate(String value) {
        return Optional.ofNullable(value)
                .filter(v -> !v.isBlank())
                .map(v -> {
                    try {
                        return LocalDate.parse(v, DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        throw new BadRequestException("Invalid date format '" + v + "'. Expected yyyy-MM-dd");
                    }
                })
                .orElse(null);
    }

    private String getOptionalCsvField(CSVRecord record, String header) {
        try {
            return record.get(header);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
