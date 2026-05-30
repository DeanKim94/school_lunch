package com.school.lunch.util;

import com.school.lunch.model.Assignment;
import com.school.lunch.model.BatchPlan;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatchPlanUtil {
    private static final Pattern START_DATE_PATTERN = Pattern.compile("\\\"startDate\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern END_DATE_PATTERN = Pattern.compile("\\\"endDate\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern PLACES_PATTERN = Pattern.compile("\\\"places\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
            "\\\\{\\s*\\\"date\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"dayOfWeek\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"places\\\"\\s*:\\s*\\[(.*?)\\]\\s*\\}" ,
            Pattern.DOTALL);    private static final Pattern TEACHER_STATS_PATTERN = Pattern.compile(
            "\\\\{\\s*\\\"teacherName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"total\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"placeCounts\\\"\\s*:\\s*\\{(.*?)\\}\\s*\\}",
            Pattern.DOTALL);
    public static void saveBatchPlan(File file, BatchPlan batchPlan) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"startDate\": \"").append(batchPlan.getStartDate()).append("\",\n");
        builder.append("  \"endDate\": \"").append(batchPlan.getEndDate()).append("\",\n");
        builder.append("  \"places\": [");
        for (int i = 0; i < batchPlan.getPlaces().size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(quote(batchPlan.getPlaces().get(i)));
        }
        builder.append("],\n");
        builder.append("  \"assignments\": [\n");
        List<Assignment> assignments = batchPlan.getAssignments();
        for (int i = 0; i < assignments.size(); i++) {
            Assignment a = assignments.get(i);
            builder.append("    {\n");
            builder.append("      \"date\": \"").append(a.getDate()).append("\",\n");
            builder.append("      \"dayOfWeek\": \"").append(escapeJson(a.getDayOfWeek())).append("\",\n");
            builder.append("      \"places\": [");
            String[] places = a.getPlaces();
            for (int j = 0; j < places.length; j++) {
                if (j > 0) builder.append(", ");
                builder.append(quote(places[j]));
            }
            builder.append("]\n");
            builder.append("    }");
            if (i < assignments.size() - 1) builder.append(",");
            builder.append("\n");
        }
        builder.append("  ],\n");
        builder.append("  \"teacherStatistics\": [\n");

        Map<String, Map<String, Integer>> teacherStats = batchPlan.getTeacherStatistics();
        int teacherIndex = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : teacherStats.entrySet()) {
            builder.append("    {\n");
            builder.append("      \"teacherName\": \"").append(escapeJson(entry.getKey())).append("\",\n");
            Map<String, Integer> counts = entry.getValue();
            builder.append("      \"total\": ").append(counts.getOrDefault("total", 0)).append(",\n");
            builder.append("      \"placeCounts\": {\n");
            int placeCountIndex = 0;
            for (Map.Entry<String, Integer> placeEntry : counts.entrySet()) {
                if (placeEntry.getKey().equals("total")) {
                    continue;
                }
                if (placeCountIndex > 0) builder.append(",\n");
                builder.append("        \"").append(escapeJson(placeEntry.getKey())).append("\": ").append(placeEntry.getValue());
                placeCountIndex++;
            }
            if (placeCountIndex > 0) builder.append("\n");
            builder.append("      }\n");
            builder.append("    }");
            if (teacherIndex < teacherStats.size() - 1) builder.append(",");
            builder.append("\n");
            teacherIndex++;
        }
        builder.append("  ]\n");
        builder.append("}\n");
        Files.writeString(file.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    public static BatchPlan loadBatchPlan(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        LocalDate startDate = parseDateValue(content, START_DATE_PATTERN)
                .orElse(null);
        LocalDate endDate = parseDateValue(content, END_DATE_PATTERN)
                .orElse(null);

        List<String> places = parseStringArray(content, PLACES_PATTERN)
                .orElse(new ArrayList<>());
        if (places.isEmpty()) {
            throw new IOException("JSON 파일에서 장소 목록을 읽어올 수 없습니다.");
        }

        List<Assignment> assignments = new ArrayList<>();
        Matcher assignmentMatcher = ASSIGNMENT_PATTERN.matcher(content);
        while (assignmentMatcher.find()) {
            LocalDate date = LocalDate.parse(assignmentMatcher.group(1));
            String dayOfWeek = unescapeJson(assignmentMatcher.group(2));
            List<String> placeValues = parseStringValueArray(assignmentMatcher.group(3));
            assignments.add(new Assignment(date, dayOfWeek, placeValues.toArray(new String[0])));
        }

        if (assignments.isEmpty()) {
            throw new IOException("JSON 파일에서 배치 정보를 읽어올 수 없습니다.");
        }

        Map<String, Map<String, Integer>> teacherStatistics = parseTeacherStatistics(content);

        if (startDate == null || endDate == null) {
            LocalDate min = assignments.stream().map(Assignment::getDate).min(LocalDate::compareTo).get();
            LocalDate max = assignments.stream().map(Assignment::getDate).max(LocalDate::compareTo).get();
            startDate = min;
            endDate = max;
        }

        return new BatchPlan(startDate, endDate, places, assignments, teacherStatistics);
    }

    private static Optional<LocalDate> parseDateValue(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return Optional.of(LocalDate.parse(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static Optional<List<String>> parseStringArray(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(parseStringValueArray(matcher.group(1)));
    }

    private static List<String> parseStringValueArray(String arrayText) {
        List<String> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(arrayText);
        while (matcher.find()) {
            values.add(unescapeJson(matcher.group(1)));
        }
        return values;
    }

    private static Map<String, Map<String, Integer>> parseTeacherStatistics(String content) {
        Map<String, Map<String, Integer>> teacherStats = new LinkedHashMap<>();
        Matcher matcher = TEACHER_STATS_PATTERN.matcher(content);
        while (matcher.find()) {
            String teacherName = unescapeJson(matcher.group(1));
            int total = Integer.parseInt(matcher.group(2));
            String placeCountsText = matcher.group(3);
            Map<String, Integer> placeCounts = parsePlaceCountsMap(placeCountsText);
            placeCounts.put("total", total);
            teacherStats.put(teacherName, placeCounts);
        }
        return teacherStats;
    }

    private static Map<String, Integer> parsePlaceCountsMap(String text) {
        Map<String, Integer> map = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(\\d+)").matcher(text);
        while (matcher.find()) {
            map.put(unescapeJson(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        return map;
    }

    private static String quote(String text) {
        return "\"" + escapeJson(text) + "\"";
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String unescapeJson(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }
}
