
package com.compassites.model.travelomatrix;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class UpdatePNRRequest {

    @JsonProperty("AppReference")
    private String appReference;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

}
