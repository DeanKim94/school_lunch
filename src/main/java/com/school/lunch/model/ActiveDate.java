package com.school.lunch.model;

import java.time.LocalDate;

public class ActiveDate {
    private LocalDate date;
    private String dayOfWeek;
    private int dayIndex; // 0=Sunday, 1=Monday... JS uses 0-6. In JS 1 is Monday.
    private boolean checked = true;

    public ActiveDate(LocalDate date, String dayOfWeek, int dayIndex) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.dayIndex = dayIndex;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
