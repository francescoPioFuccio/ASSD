package Entity;

public class Museum {
    private String name;
    private double latitude;
    private double longitude;
    private int estimatedVisitTime; // in minutes

    public Museum(String name, double latitude, double longitude, int estimatedVisitTime) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedVisitTime = estimatedVisitTime;
    }

    // Getters
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getEstimatedVisitTime() { return estimatedVisitTime; }
}
