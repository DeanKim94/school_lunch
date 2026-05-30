package com.school.lunch.model;

public class Teacher {
    private int id;
    private String name;
    private String[] lessons = new String[5]; // 월(0) ~ 금(4) 4교시 학급
    private boolean selected = true;
    
    // For algorithm
    private int futureAvail;
    private double todayRandom;

    public Teacher(int id, String name, String[] lessons) {
        this.id = id;
        this.name = name;
        this.lessons = lessons;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String[] getLessons() {
        return lessons;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getFutureAvail() {
        return futureAvail;
    }

    public void setFutureAvail(int futureAvail) {
        this.futureAvail = futureAvail;
    }

    public double getTodayRandom() {
        return todayRandom;
    }

    public void setTodayRandom(double todayRandom) {
        this.todayRandom = todayRandom;
    }
}
