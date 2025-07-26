package com.example.healthcare;

public class PatientLog {
    private String patientName;
    private String date;
    private int steps;
    private int waterIntake;
    private String sleepDuration;

    public PatientLog() {
        // Required empty constructor for Firebase
    }

    public PatientLog(String date, String patientName, int steps, int waterIntake, String sleepDuration) {
        this.date = date;
        this.patientName = patientName;
        this.steps = steps;
        this.waterIntake = waterIntake;
        this.sleepDuration = sleepDuration;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getWaterIntake() {
        return waterIntake;
    }

    public void setWaterIntake(int waterIntake) {
        this.waterIntake = waterIntake;
    }

    public String getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(String sleepDuration) {
        this.sleepDuration = sleepDuration;
    }
} 