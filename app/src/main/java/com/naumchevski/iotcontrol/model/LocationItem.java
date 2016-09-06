package com.naumchevski.iotcontrol.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Random;

public class LocationItem implements Serializable {

    private int id;
    private float longitude;
    private float latitude;

    public LocationItem() {

    }

    public LocationItem(float longitude,float latitude) {
        this.setId(new Random().nextInt(100000) + 1);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
