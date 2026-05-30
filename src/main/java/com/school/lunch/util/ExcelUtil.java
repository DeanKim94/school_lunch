package com.school.lunch.util;

import com.school.lunch.model.Assignment;
import com.school.lunch.model.Teacher;
import com.school.lunch.service.AssignerService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelUtil {

    public static List<Teacher> readTeachersFromExcel(File file) throws Exception {
        List<Teacher> teachers = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int idCounter = 1;
            
            // 월, 화, 수, 목, 금 4교시 인덱스 (E, K, R, X, AE)
            int[] colIndices = {4, 10, 17, 23, 30}; 
            Pattern namePattern = Pattern.compile("[가-힣]+");

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(0);
                if (nameCell == null) continue;

                String nameRaw = getCellValueAsString(nameCell).replaceAll("\\s+", "");
                if (nameRaw.isEmpty() || nameRaw.contains("교사") || nameRaw.contains("성명")) {
                    continue;
                }

                Matcher matcher = namePattern.matcher(nameRaw);
                String name = matcher.find() ? matcher.group() : nameRaw;

                String[] lessons = new String[5];
                for (int j = 0; j < 5; j++) {
                    Cell classCell = row.getCell(colIndices[j]);
                    String classRaw = getCellValueAsString(classCell);
                    Matcher m = Pattern.compile("\\d{3}").matcher(classRaw);
                    if (m.find()) {
                        lessons[j] = m.group();
                    } else {
                        lessons[j] = null;
                    }
                }

                teachers.add(new Teacher(idCounter++, name, lessons));
            }
        }
        return teachers;
    }

    public static void writeResultsToExcel(File file, List<Assignment> results, List<String> places, Map<String, Map<String, Integer>> teacherCounts) throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            
            // 프리미엄 테마 색상 정의
            byte[] rgbHeader = new byte[]{(byte) 30, (byte) 64, (byte) 175}; // Deep Blue (#1E40AF)
            XSSFColor headerColor = new XSSFColor(rgbHeader, null);

            byte[] rgbAlt = new byte[]{(byte) 241, (byte) 245, (byte) 249}; // Slate 100 (#F1F5F9)
            XSSFColor altColor = new XSSFColor(rgbAlt, null);

            byte[] rgbTotal = new byte[]{(byte) 254, (byte) 243, (byte) 199}; // Amber 100 (#FEF3C7) - 총합 셀 강조
            XSSFColor totalColor = new XSSFColor(rgbTotal, null);

            // Font 설정
            Font headerFont = wb.createFont();
            headerFont.setFontName("맑은 고딕");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            Font dataFont = wb.createFont();
            dataFont.setFontName("맑은 고딕");
            dataFont.setFontHeightInPoints((short) 10);

            Font totalFont = wb.createFont();
            totalFont.setFontName("맑은 고딕");
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 10);

            // 헤더 스타일
            XSSFCellStyle headerStyle = (XSSFCellStyle) wb.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(headerColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(headerStyle);
            headerStyle.setFont(headerFont);

            // 기본 데이터 스타일 (가운데 정렬)
            XSSFCellStyle centerStyle = (XSSFCellStyle) wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(centerStyle);
            centerStyle.setFont(dataFont);

            // 격행 데이터 스타일
            XSSFCellStyle altStyle = (XSSFCellStyle) wb.createCellStyle();
            altStyle.setAlignment(HorizontalAlignment.CENTER);
            altStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            altStyle.setFillForegroundColor(altColor);
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(altStyle);
            altStyle.setFont(dataFont);

            // 총합(강조) 스타일
            XSSFCellStyle totalStyle = (XSSFCellStyle) wb.createCellStyle();
            totalStyle.setAlignment(HorizontalAlignment.CENTER);
            totalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalStyle.setFillForegroundColor(totalColor);
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(totalStyle);
            totalStyle.setFont(totalFont);

            // ==========================================
            // Sheet 1: 급식지도 배치 최종
            // ==========================================
            Sheet ws1 = wb.createSheet("급식지도 배치 최종");
            ws1.setDefaultRowHeightInPoints(24);
            
            Row header1 = ws1.createRow(0);
            header1.setHeightInPoints(28);
            
            List<String> h1 = new ArrayList<>();
            h1.add("날짜");
            h1.add("요일");
            h1.addAll(places);
            
            for(int i = 0; i < h1.size(); i++) {
                Cell cell = header1.createCell(i);
                cell.setCellValue(h1.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            int rIdx = 1;
            for (Assignment r : results) {
                Row row = ws1.createRow(rIdx++);
                row.setHeightInPoints(22);
                
                XSSFCellStyle currentStyle = (rIdx % 2 == 0) ? centerStyle : altStyle;
                
                Cell c0 = row.createCell(0);
                c0.setCellValue(r.getDate().toString());
                c0.setCellStyle(currentStyle);
                
                Cell c1 = row.createCell(1);
                c1.setCellValue(r.getDayOfWeek());
                c1.setCellStyle(currentStyle);
                
                String[] assignedPlaces = r.getPlaces();
                for (int i = 0; i < places.size(); i++) {
                    Cell cPlace = row.createCell(i + 2);
                    cPlace.setCellValue(i < assignedPlaces.length ? assignedPlaces[i] : "");
                    cPlace.setCellStyle(currentStyle);
                }
            }

            for (int i = 0; i < h1.size(); i++) {
                ws1.autoSizeColumn(i);
                ws1.setColumnWidth(i, ws1.getColumnWidth(i) + 1200);
            }

            // ==========================================
            // Sheet 2: 교사별 장소 통계 (1단 구성)
            // ==========================================
            Sheet ws2 = wb.createSheet("교사별 장소 통계");
            ws2.setDefaultRowHeightInPoints(24);
            
            List<String> h2 = new ArrayList<>();
            h2.add("교사명");
            h2.addAll(places);
            h2.add("총합");
            
            Row header2 = ws2.createRow(0);
            header2.setHeightInPoints(28);
            
            for (int i = 0; i < h2.size(); i++) {
                Cell cell = header2.createCell(i);
                cell.setCellValue(h2.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            List<Map.Entry<String, Map<String, Integer>>> statList = new ArrayList<>(teacherCounts.entrySet());
            statList.sort((a, b) -> b.getValue().get("total").compareTo(a.getValue().get("total")));
            
            int statRIdx = 1;
            for (Map.Entry<String, Map<String, Integer>> entry : statList) {
                Row row = ws2.createRow(statRIdx++);
                row.setHeightInPoints(22);
                XSSFCellStyle currentStyle = (statRIdx % 2 == 0) ? centerStyle : altStyle;
                
                Cell cellName = row.createCell(0);
                cellName.setCellValue(entry.getKey());
                cellName.setCellStyle(currentStyle);
                
                Map<String, Integer> counts = entry.getValue();
                int cIdx = 1;
                for (String place : places) {
                    Cell cVal = row.createCell(cIdx++);
                    cVal.setCellValue(counts.getOrDefault(place, 0));
                    cVal.setCellStyle(currentStyle);
                }
                Cell cTotal = row.createCell(cIdx);
                cTotal.setCellValue(counts.getOrDefault("total", 0));
                cTotal.setCellStyle(totalStyle); // 총합 셀 강조
            }

            for (int i = 0; i < h2.size(); i++) {
                ws2.autoSizeColumn(i);
                ws2.setColumnWidth(i, ws2.getColumnWidth(i) + 1200);
            }
            
            wb.write(fos);
        }
    }

    private static void setThinBorders(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((int)cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: 
                try {
                    return cell.getStringCellValue();
                } catch(Exception e) {
                    return String.valueOf((int)cell.getNumericCellValue());
                }
            default: return "";
        }
    }
}
