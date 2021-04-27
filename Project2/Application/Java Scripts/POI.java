package omada6.katanemimena.katanemimenaapp;

import java.io.Serializable;

public class POI implements Serializable {
    private int ID;
    private double longitude, latitude;
    private String POI_name,photos, category;

    public POI(int ID, double latitude, double longitude, String photos, String POI_name, String category ) {
        this.ID = ID;
        this.longitude = longitude;
        this.latitude = latitude;
        this.POI_name = POI_name;
        this.photos = photos;
        this.category = category;
    }

    public int getID() {
        return ID;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getPOI_name() {
        return POI_name;
    }

    public String getPhotos() {
        return photos;
    }

    public String getCategory() {
        return category;
    }
}
