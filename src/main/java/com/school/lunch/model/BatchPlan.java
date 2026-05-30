package com.school.lunch.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class BatchPlan {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> places;
    private List<Assignment> assignments;
    private Map<String, Map<String, Integer>> teacherStatistics;

    public BatchPlan(LocalDate startDate, LocalDate endDate, List<String> places, List<Assignment> assignments,
                     Map<String, Map<String, Integer>> teacherStatistics) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.places = places;
        this.assignments = assignments;
        this.teacherStatistics = teacherStatistics;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<String> getPlaces() {
        return places;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public Map<String, Map<String, Integer>> getTeacherStatistics() {
        return teacherStatistics;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }
}
