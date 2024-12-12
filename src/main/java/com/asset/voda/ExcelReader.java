package com.asset.voda;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for reading metadata from Excel files and generating Excel reports.
 * Provides methods to extract metadata for documents and create detailed reports in Excel format.
 */
public class ExcelReader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    // Reads metadata from the given Excel file and maps it to document names
    public static Map<String, Map<String, String>> readMetadataFromExcel(File excelFile) throws IOException {
        Map<String, Map<String, String>> documentMetadataMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String documentName = getCellValueAsString(row.getCell(0)).trim();
                Map<String, String> metadata = new HashMap<>();

                for (int col = 1; col < row.getLastCellNum(); col++) {
                    metadata.put(sheet.getRow(0).getCell(col).getStringCellValue().trim(),
                            getCellValueAsString(row.getCell(col)).trim());
                }

                documentMetadataMap.put(documentName, metadata);
            }
        }

        return documentMetadataMap;
    }

    // Generates an Excel report based on the provided metadata list and saves it to a file
    public static void generateExcelReport(List<Map<String, Object>> metadataList, File reportFile) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Document Upload Report");

            if (metadataList.isEmpty()) {
                logger.warn("No metadata to generate a report.");
                return;
            }

            // Create header row
            Row headerRow = sheet.createRow(0);
            Map<String, Object> sampleMetadata = metadataList.get(0); // Assume all maps have the same keys
            List<String> headers = new ArrayList<>(sampleMetadata.keySet());
            for (int i = 0; i < headers.size(); i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers.get(i));
            }

            // Populate rows with metadata
            int rowNum = 1;
            for (Map<String, Object> metadata : metadataList) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.createCell(i);
                    String value = String.valueOf(metadata.getOrDefault(headers.get(i), ""));
                    cell.setCellValue(value);
                }
            }

            // Write to the Excel file
            try (FileOutputStream fileOut = new FileOutputStream(reportFile)) {
                workbook.write(fileOut);
                logger.info("Excel report generated successfully: {}", reportFile.getName());
            }
        } catch (IOException e) {
            logger.error("Error generating Excel report", e);
        }
    }

    // Helper method to get a cell value as a string
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // For date cells
                } else {
                    // Format numbers as strings to preserve decimal precision
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        // If it is a whole number, format it without decimals
                        return String.format("%d", (long) numericValue);
                    } else {
                        // Otherwise, keep the decimal precision
                        return String.format("%f", numericValue);
                    }
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}