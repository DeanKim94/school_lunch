package com.school.lunch.model;

import java.time.LocalDate;

public class Assignment {
    private LocalDate date;
    private String dayOfWeek;
    private String[] places;

    public Assignment(LocalDate date, String dayOfWeek, String[] places) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.places = places;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String[] getPlaces() {
        return places;
    }

    public void setPlaces(String[] places) {
        this.places = places;
    }
}
