package com.naumchevski.iotcontrol.model;

import com.google.gson.annotations.SerializedName;

public class CounterDTO {

    @SerializedName("name")
    private String name;

    @SerializedName("privKey")
    private String controlId;

    @SerializedName("pubKey")
    private String shareId;

    @SerializedName("value")
    private int value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
