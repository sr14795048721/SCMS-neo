package com.scms.core.adminuser.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStudentExcelService {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SHEET_NAME = "students";
    private static final String[] HEADERS = {
            "用户名", "邮箱", "姓名", "学号", "年级", "班级", "手机号", "个人简介", "是否启用"
    };

    public AdminUserExcelFileContent buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeHeader(sheet, workbook);
            writeRow(sheet.createRow(1), new String[]{
                    "student20260001",
                    "student20260001@scms.local",
                    "张三",
                    "20260001",
                    "高一",
                    "3",
                    "13800000000",
                    "示例简介",
                    "是"
            });
            autosize(sheet);
            return new AdminUserExcelFileContent(writeWorkbook(workbook), "学生导入模板.xlsx", CONTENT_TYPE);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成学生导入模板失败");
        }
    }

    public AdminUserExcelFileContent buildExport(List<AdminStudentExportRow> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeHeader(sheet, workbook);
            for (int index = 0; index < rows.size(); index += 1) {
                AdminStudentExportRow row = rows.get(index);
                writeRow(sheet.createRow(index + 1), new String[]{
                        row.username(),
                        row.email(),
                        row.displayName(),
                        row.studentNo(),
                        row.gradeLabel(),
                        row.className(),
                        row.phone(),
                        row.bio(),
                        row.enabledLabel()
                });
            }
            autosize(sheet);
            String fileName = "学生数据导出-" + FILE_DATE_FORMATTER.format(LocalDate.now(SHANGHAI_ZONE)) + ".xlsx";
            return new AdminUserExcelFileContent(writeWorkbook(workbook), fileName, CONTENT_TYPE);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出学生数据失败");
        }
    }

    public List<AdminStudentImportRow> parseImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入模板不正确");
            }

            validateHeader(sheet.getRow(0));
            DataFormatter formatter = new DataFormatter();
            List<AdminStudentImportRow> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex += 1) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, formatter)) {
                    continue;
                }

                rows.add(new AdminStudentImportRow(
                        rowIndex + 1,
                        readCell(row, 0, formatter),
                        readCell(row, 1, formatter),
                        readCell(row, 2, formatter),
                        readCell(row, 3, formatter),
                        readCell(row, 4, formatter),
                        readCell(row, 5, formatter),
                        readCell(row, 6, formatter),
                        readCell(row, 7, formatter),
                        readCell(row, 8, formatter)
                ));
            }
            return rows;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入文件格式不正确，请使用模板文件");
        }
    }

    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入模板不正确");
        }

        DataFormatter formatter = new DataFormatter();
        for (int index = 0; index < HEADERS.length; index += 1) {
            String actual = readCell(headerRow, index, formatter);
            if (!HEADERS[index].equals(actual)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入模板不正确，请先下载最新模板");
            }
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        for (int index = 0; index < HEADERS.length; index += 1) {
            if (!readCell(row, index, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private void writeHeader(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int index = 0; index < HEADERS.length; index += 1) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRow(Row row, String[] values) {
        for (int index = 0; index < values.length; index += 1) {
            row.createCell(index).setCellValue(values[index]);
        }
    }

    private void autosize(Sheet sheet) {
        for (int index = 0; index < HEADERS.length; index += 1) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 1024, 16000));
        }
    }

    private byte[] writeWorkbook(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public record AdminStudentImportRow(
            int rowNumber,
            String username,
            String email,
            String displayName,
            String studentNo,
            String grade,
            String className,
            String phone,
            String bio,
            String enabled
    ) {
    }

    public record AdminStudentExportRow(
            String username,
            String email,
            String displayName,
            String studentNo,
            String gradeLabel,
            String className,
            String phone,
            String bio,
            String enabledLabel
    ) {
    }
}
