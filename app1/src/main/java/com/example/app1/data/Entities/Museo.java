package com.example.app1.data.Entities;

public class Museo {
    private String name;
    private double latitude;
    private double longitude;
    private int estimatedVisitTime;

    // Costruttori
    public Museo() {}

    public Museo(String name, double latitude, double longitude, int estimatedVisitTime) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedVisitTime = estimatedVisitTime;
    }

    // Getter e Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getEstimatedVisitTime() {
        return estimatedVisitTime;
    }

    public void setEstimatedVisitTime(int estimatedVisitTime) {
        this.estimatedVisitTime = estimatedVisitTime;
    }
}