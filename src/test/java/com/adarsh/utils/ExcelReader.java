package com.adarsh.utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads {@code .xlsx} test data into rows keyed by the header names.
 *
 * <p>Returning {@code List<Map<String,String>>} rather than positional arrays means adding a
 * column to the spreadsheet never silently shifts an existing test's data.
 */
public final class ExcelReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelReader() {
        // utility holder
    }

    /** Reads one sheet, using the first row as headers. */
    public static List<Map<String, String>> readSheet(String fileName, String sheetName) {
        try (var stream = open(fileName);
                var workbook = new XSSFWorkbook(stream)) {

            var sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Sheet '" + sheetName + "' not found in " + fileName
                                + ". Available: " + sheetNames(workbook));
            }

            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            var headers = new ArrayList<String>();
            headerRow.forEach(cell -> headers.add(cellValue(cell)));

            var rows = new ArrayList<Map<String, String>>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null || isBlank(row)) {
                    continue;
                }
                var record = new LinkedHashMap<String, String>();
                for (int column = 0; column < headers.size(); column++) {
                    record.put(headers.get(column), cellValue(row.getCell(column)));
                }
                rows.add(record);
            }
            return List.copyOf(rows);

        } catch (IOException e) {
            throw new IllegalStateException("Could not read workbook: " + fileName, e);
        }
    }

    /** Convenience for the common "amount" style column. */
    public static BigDecimal money(Map<String, String> row, String column) {
        var raw = row.get(column);
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(raw.replace("$", "").replace(",", "").trim());
    }

    private static String cellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        // DataFormatter renders numbers the way the sheet shows them, so account numbers do not
        // come back as "13233.0".
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static boolean isBlank(Row row) {
        for (var cell : row) {
            if (!cellValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sheetNames(XSSFWorkbook workbook) {
        var names = new ArrayList<String>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names.add(workbook.getSheetName(i));
        }
        return names;
    }

    private static InputStream open(String fileName) {
        var path = fileName.startsWith("testdata/") ? fileName : "testdata/" + fileName;
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException(
                    "Workbook not found on the classpath: " + path
                            + ". Expected it under src/test/resources/testdata.");
        }
        return stream;
    }
}
