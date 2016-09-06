package com.naumchevski.iotcontrol.model;

import java.io.Serializable;

public class CounterItem implements Serializable {
    public final static String MAX_VALUE_STRING = "1023";
    public final static String MIN_VALUE_STRING = "0";
    public final static int MAX_VALUE_INT = 1023;
    public final static int MIN_VALUE_INT = 0;

    private String name;
    private int itemType;
    private String controlId;
    private String shareId;
    private int value;
    private  LocationItem locationItem;

    public static final int COUNTER_ITEM_SWITCH = 0;
    public static final int COUNTER_ITEM_SEEK_BAR = 1;

    public CounterItem(String name, int itemType, String controlId, String shareId) {
        this.name = name;
        this.itemType = itemType;
        this.controlId = controlId;
        this.shareId = shareId;
        this.value = 0;
    }

    public CounterItem(String name, int itemType, String controlId, String shareId, int value) {
        this.name = name;
        this.itemType = itemType;
        this.controlId = controlId;
        this.shareId = shareId;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public LocationItem getLocationItem() {
        return locationItem;
    }

    public void setLocationItem(LocationItem locationItem) {
        this.locationItem = locationItem;
    }
}

